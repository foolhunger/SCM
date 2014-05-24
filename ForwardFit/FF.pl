#!/usr/bin/perl

# Requirements:
# Subversion must be available
# FF Rules must use same Name/Value pairs as in CQ
# Downstream CQ field "NextVersion" must have option "Forward_Fit"
# Downstream CQ field "Defect Type" must have option "Other"


use strict;
use warnings;
use utf8;
use CQPerlExt;
use File::Copy();


open FH_ERROR, '>ff_errorlog.txt' or die "Error: unable to open file 'ff_errorlog.txt': $!\n";
if(! open FH_LOG, '>ff_log.txt')
{
	print FH_ERROR "Line: " . __LINE__ . " Error: unable to open file 'ff_log.txt': $!\n";
	&PostActions;
	exit(1);
}


my $FFPrefix = "[FFIT]";


print FH_LOG "==> Initialize FF Log folders\n";
my $logDir = &InitFFLog;
print FH_LOG "==> FF LogDir: $logDir\n";
print FH_LOG "\n\n";


print FH_LOG "==> Read FF Rule paths from 'ffRulePaths.txt'\n";
my %ffRulePaths = &GetFFRulePaths;
print FH_LOG "==> Forward Fit Rule paths found:\n";
foreach (keys %ffRulePaths)
{
	print FH_LOG "$_ ==> $ffRulePaths{$_}\n";
}
print FH_LOG "\n\n";


print FH_LOG "==> Create CQ session and login\n\n";
my $FFSession = CQSession::Build();
eval {$FFSession->UserLogon('build_admin', 'eBao2008', 'CRDB', '')};
if($@)
{
	print FH_LOG   "Line: " . __LINE__ . " Error: unable to login to CQ database 'CRDB': $@\n";
	print FH_ERROR "Line: " . __LINE__ . " Error: unable to login to CQ database 'CRDB': $@\n";
	&PostActions;
	exit(1);
}


# For each ffRule files, parse (up|down)stream rules (email optional), then invoking &FFManager
my %CQFieldsHash;
my %rules_upstream;
my %rules_downstream;
my @email_receivers;
print FH_LOG "==> Iterating through each FF Rule files\n";
foreach my $ffRulePath (keys %ffRulePaths)
{
	chomp $ffRulePath;
	print FH_LOG "==> Processing '$ffRulePath'\n";
	my @ffRules = &GetffRules($ffRulePath);
	my $suffix = 1;
	$suffix = "0" . $suffix if $suffix < 10;
	my $ffRuleFile = $ffRulePaths{$ffRulePath} . "_$suffix";
	while(&ParseffRules(\@ffRules, $ffRuleFile))
	{
		&LoggingFFRules;
		print FH_LOG "==> Invoking &FFManager\n";
		&FFManager($ffRuleFile);
		print FH_LOG "==> Exit &FFManager\n";
		print FH_LOG "\n\n";
		$suffix += 1;
		$suffix = "0" . $suffix if $suffix < 10;
		$ffRuleFile = $ffRulePaths{$ffRulePath} . "_$suffix";
	}
}


print FH_LOG "==> Unbuild CQ session\n";
&UnbuildCQSession;


# Archive logs & Send notification email
&PostActions;


# Subroutine Definitions
sub InitFFLog
{
	if((! -d "FFlogs") and (! mkdir "FFlogs"))
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to mkdir 'FFlogs': $!\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to mkdir 'FFlogs': $!\n";
		&PostActions;
		exit(1);
	}
	my @time = localtime();
	my $year = $time[5] + 1900;
	my $month = ($time[4] + 1) >= 10 ? $time[4] + 1 : "0" . ($time[4] + 1);
	my $day = $time[3] >= 10 ? $time[3] : "0" . $time[3];
	my $logDir = $year . $month . $day . "\@" . "$time[2]h" . "$time[1]m" . "$time[0]s";
	if(! mkdir "FFlogs/$logDir")
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to mkdir 'FFlogs/$logDir': $!\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to mkdir 'FFlogs/$logDir': $!\n";
		&PostActions;
		exit(1);
	}
	return $logDir;
}


sub LoggingFFRules
{
	print FH_LOG "==> Upstream FF Rules below:\n";
	foreach (keys %rules_upstream)
	{
		chomp;
		print FH_LOG "### $_: $rules_upstream{$_}#\n";
	}

	print FH_LOG "==> Downstream FF Rules below:\n";
	foreach (keys %rules_downstream)
	{
		chomp;
		print FH_LOG "### $_: $rules_downstream{$_}#\n";
	}

	print FH_LOG "==> Email Receivers below:\n";
	foreach (@email_receivers)
	{
		chomp;
		print FH_LOG "$_;";
	}
	print FH_LOG "\n";
}


sub InitCQFieldsHash
{
	# FinishDate: 2013-02-15 00:00:00
	# ChildCR:  CRDB00798027\nCRDB00798028
	%CQFieldsHash = ();
	$CQFieldsHash{'id'} = '';
	$CQFieldsHash{'Headline'} = 'default headline';
	$CQFieldsHash{'Description'} = 'default description';
	$CQFieldsHash{'Project'} = '';
	$CQFieldsHash{'BaseVersion'} = '';
	$CQFieldsHash{'NextVersion'} = '';
	$CQFieldsHash{'Priority'} = '3-Normal Queue';
	$CQFieldsHash{'Severity'} = '3-Medium';
	$CQFieldsHash{'CRType'} = 'Defect';
	$CQFieldsHash{'CRProperty'} = 'Internal Defect';
	$CQFieldsHash{'Detected_Env'} = '';
	$CQFieldsHash{'DiscoverPhase'} = 'Development';
	$CQFieldsHash{'Owner'} = 'build_admin';
	$CQFieldsHash{'State'} = '';
	$CQFieldsHash{'Module'} = '';
	$CQFieldsHash{'ReviewPeer'} = 'build_admin';
	$CQFieldsHash{'Verifier'} = 'build_admin';
	$CQFieldsHash{'ReqID'} = '';
	$CQFieldsHash{'RequestBy'} = '';
	$CQFieldsHash{'EstimateEffort'} = '0';
	$CQFieldsHash{'dtype'} = '';
	$CQFieldsHash{'Milestone'} = '';
	$CQFieldsHash{'FinishDate'} = '2011-08-09';
}


sub GetFFRulePaths
{
	if(! open FH_RULE, '<ffRulePaths.txt')
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to open file 'ffRulePaths.txt': $!\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to open file 'ffRulePaths.txt': $!\n";
		&PostActions;
		exit(1);
	}
	my %ffRulePaths;
	while(my $rule = <FH_RULE>)
	{
		chomp $rule;
		next if $rule =~ m/\A\s*#/;
		next if $rule =~ m/\A\s*\Z/;
		$rule =~ s/\\/\//g;
		my @tmp = split /\|/, $rule;
		if($tmp[0] and $tmp[0] =~ m/\A\s*(.+\/(.+)\.(?:txt|config))\s*\Z/i)
		{
			my ($rule_path, $rule_name) = ($1, $2);		
			$rule_name =~ s/\s/_/g;
			# Subversion stuff
			if($tmp[1] and $tmp[1] =~ m/\A\s*SVN\s*=\s*(?:True|T|Yes|Y)\s*\Z/i)
			{
				my $cmd = "svn update \"$rule_path\" >\"FFlogs/$logDir/svn_update_${rule_name}.txt\"";
				system($cmd);
			}
			$ffRulePaths{$rule_path} = $rule_name;
		}
	}
	close FH_RULE;
	return %ffRulePaths;
}


# Return a list of FF Rules on success, () otherwise
sub GetffRules
{
	my ($ffRuleFile) = @_;

	if(! open FH_RULE, "<$ffRuleFile")
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to open ffRuleFile '$ffRuleFile': $!\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to open ffRuleFile '$ffRuleFile': $!\n";
		return ();
	}
	my @ffRules;
	while(my $ffRule = <FH_RULE>)
	{
		chomp $ffRule;
		push @ffRules, $1 if $ffRule =~ m/\A\s*(upstream\s*:.*?)\s*\Z/i;
		push @ffRules, $1 if $ffRule =~ m/\A\s*(downstream\s*:.*?)\s*\Z/i;
		push @ffRules, $1 if $ffRule =~ m/\A\s*(mailto\s*:.*?)\s*\Z/i;
	}
	close FH_RULE;
	return @ffRules;
}


# Return 1 on success, 0 otherwise
sub ParseffRules
{
	# array reference
	my ($ffRules, $ffRuleFile) = @_;

	return 0 if !scalar @$ffRules;

	%rules_upstream = ();
	%rules_downstream = ();
	@email_receivers = ();
	# upstream:    Project=GS3.6 Maintenance;Milestone=3.6.1;State=Reviewd|Resolved;DateResolve>2013-03-18;
	# downstream:  Project=GS3.7;State=Assigned
	# mailto:      jackie.xiao@ebaotech.com;reach.li@ebaotech.com
	foreach my $keyword (qw(upstream downstream))
	{
		if(!scalar @$ffRules)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: invalid ffRuleFile: '$ffRuleFile': $keyword rule is missing\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: invalid ffRuleFile: '$ffRuleFile': $keyword rule is missing\n";
			return 0;
		}
		elsif(@$ffRules[0] =~ m/\A\s*\Q$keyword\E\s*:\s*(.+?)\s*\Z/i)
		{
			my @tmp = split /;/, $1;
			foreach my $ffRule (@tmp)
			{
				chomp $ffRule;
				next if $ffRule =~ m/\A\s*\Z/;
				$ffRule = $1 if $ffRule =~ m/\A\s*(.+?)\s*\Z/;
				if($ffRule =~ m/\A\s*(.+?)\s*(=|!=|<>|>|>=|<|<=)\s*(.+?)\s*\Z/)
				{
					my $op = $2;
					$op .= " " if length $op == 1;
					$rules_upstream{$1} = "$op$3" if $keyword eq "upstream";
					$rules_downstream{$1} = "$3" if $keyword eq "downstream";
				}
				else
				{
					print FH_LOG   "Line: " . __LINE__ . " Error: invalid ffRuleFile: '$ffRuleFile': @$ffRules[0]\n";
					print FH_ERROR "Line: " . __LINE__ . " Error: invalid ffRuleFile: '$ffRuleFile': @$ffRules[0]\n";
					return 0;
				}
			}
			shift @$ffRules;
		}
		else
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: invalid ffRuleFile: '$ffRuleFile': @$ffRules[0]\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: invalid ffRuleFile: '$ffRuleFile': @$ffRules[0]\n";
			return 0;
		}
	}
	if(@$ffRules[0] and @$ffRules[0] =~ m/\A\s*mailto\s*:\s*(.+?)\s*\Z/i)
	{
		my @tmp = split /;/, $1;
		foreach my $email_receiver (@tmp)
		{
			push @email_receivers, $1 if $email_receiver =~ m/\A\s*(\S+\@\S+)\s*\Z/;
		}
		shift @$ffRules;
	}
	if(exists $rules_upstream{'Project'} and exists $rules_downstream{'Project'})
	{
		return 1;
	}
	else
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: invalid ffRuleFile: '$ffRuleFile': FF Rule 'Project' is missing\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: invalid ffRuleFile: '$ffRuleFile': FF Rule 'Project' is missing\n";
		return 0;
	}
}


# Get FF candidate CQs, output to CSV, return CQ record amount
sub GetFFCandidates
{
	my ($ffRuleFile) = @_;
	&InitCQFieldsHash;

	# Build CQ query based on %rules_upstream
	# "Defect" below corresponds to a "record type" in the schema
	my $FFQuery;
	eval {$FFQuery = $FFSession->BuildQuery("Defect")};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'BuildQuery' raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'BuildQuery' raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	foreach my $CQfield (sort keys %CQFieldsHash)
	{
		chomp $CQfield;
		# Skip CQ field "Description"
		next if $CQfield eq "Description";
		eval {$FFQuery->BuildField($CQfield)};
		if($@)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'BuildField' raised exceptions: $@\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'BuildField' raised exceptions: $@\n";
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}
	}
	my $FFQueryOp;
	eval {$FFQueryOp = $FFQuery->BuildFilterOperator($CQPerlExt::CQ_BOOL_OP_AND)};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'BuildFilterOperator' raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'BuildFilterOperator' raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	foreach my $rule (keys %rules_upstream)
	{
		chomp $rule;
		my $op = substr $rules_upstream{$rule}, 0, 2;
		$op = $CQPerlExt::CQ_COMP_OP_EQ  if $op eq "= ";
		$op = $CQPerlExt::CQ_COMP_OP_NEQ if $op eq "<>";
		$op = $CQPerlExt::CQ_COMP_OP_NEQ if $op eq "!=";
		$op = $CQPerlExt::CQ_COMP_OP_LT  if $op eq "< ";
		$op = $CQPerlExt::CQ_COMP_OP_LTE if $op eq "<=";
		$op = $CQPerlExt::CQ_COMP_OP_GT  if $op eq "> ";
		$op = $CQPerlExt::CQ_COMP_OP_GTE if $op eq ">=";
		$rules_upstream{$rule} = substr $rules_upstream{$rule}, 2;

		my @tmp = split /\|/, $rules_upstream{$rule};
		@tmp = grep {!($_ =~ m/\A\s*\Z/)} @tmp;
		foreach my $tmp (@tmp)
		{
			$tmp = $1 if $tmp =~ m/\A\s*(.+?)\s*\Z/;
		}
		if(scalar @tmp == 1)
		{
			eval {$FFQueryOp->BuildFilter($rule, $op, [$tmp[0]])};
			if($@)
			{
				print FH_LOG   "Line: " . __LINE__ . " Error: unable to add CQ query filter '$rule': $@\n";
				print FH_ERROR "Line: " . __LINE__ . " Error: unable to add CQ query filter '$rule': $@\n";
				&UnbuildCQSession;
				&PostActions;
				exit(1);
			}
		}
		elsif(scalar @tmp > 1)
		{
			eval {$FFQueryOp->BuildFilter($rule, $CQPerlExt::CQ_COMP_OP_IN, \@tmp)};
			if($@)
			{
				print FH_LOG   "Line: " . __LINE__ . " Error: unable to add CQ query filter '$rule': $@\n";
				print FH_ERROR "Line: " . __LINE__ . " Error: unable to add CQ query filter '$rule': $@\n";
				&UnbuildCQSession;
				&PostActions;
				exit(1);
			}
		}
		else
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: invalid ffRule value $rule='$rules_upstream{$rule}'\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: invalid ffRule value $rule='$rules_upstream{$rule}'\n";
			&UnbuildCQSession;
			&PostActions;
			exit(1); 
		}
	}
	my $FFQueryResultSet;
	eval {$FFQueryResultSet = $FFSession->BuildResultSet($FFQuery)};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'BuildResultSet' raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'BuildResultSet' raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	eval {$FFQueryResultSet->Execute()};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'Execute' raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'Execute' raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}

	if(! open FH_CSV, ">FFlogs/$logDir/FFQuery_$ffRuleFile.csv")
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to open CSV file 'FFQuery_$ffRuleFile.csv': $!\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to open CSV file 'FFQuery_$ffRuleFile.csv': $!\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}

	my $FFQueryResCount = 0;
	my $FFQueryColCount;
	eval {$FFQueryColCount = $FFQueryResultSet->GetNumberOfColumns};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'GetNumberOfColumns' raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'GetNumberOfColumns' raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	my $FFQueryFetchRes;
	eval {$FFQueryFetchRes = $FFQueryResultSet->MoveNext};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'MoveNext' raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'MoveNext' raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}

	while($FFQueryFetchRes == $CQPerlExt::CQ_SUCCESS)
	{
		$FFQueryResCount += 1;
		my $ColCount = 1;
		while($ColCount <= $FFQueryColCount)
		{
			my $ColValue;
			eval {$ColValue = $FFQueryResultSet->GetColumnValue($ColCount)};
			if($@)
			{
				print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'GetColumnValue' raised exceptions: $@\n";
				print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'GetColumnValue' raised exceptions: $@\n";
				&UnbuildCQSession;
				&PostActions;
				exit(1);
			}
			# For CSV output purpose, replacing comma with semicolon
			$ColValue =~ s/,/;/g;
			print FH_CSV $ColValue;
			$ColCount += 1;
			if($ColCount <= $FFQueryColCount)
			{
				print FH_CSV ",";
			}
		}
		print FH_CSV "\n";
		eval {$FFQueryFetchRes = $FFQueryResultSet->MoveNext};
		if($@)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'MoveNext' raised exceptions: $@\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'MoveNext' raised exceptions: $@\n";
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}
	}
	close FH_CSV;
	return $FFQueryResCount;
}


# Retrieve and return CQ field value
sub GetCQFieldValue
{
	my ($CQEntity, $CQFieldName) = @_;
	my $res;
	eval {$res = $CQEntity->GetFieldValue($CQFieldName)};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'GetFieldValue' ($CQFieldName) raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'GetFieldValue' ($CQFieldName) raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	eval {$res = $res->GetValue()};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'GetValue' ($CQFieldName) raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'GetValue' ($CQFieldName) raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	return $res;
}


# Check whether CQ has been {FF}ed
sub HasBeenFFed
{
	my ($ChildCR) = @_;
	return 0 if ! $ChildCR;
	my @ChildCR = split /\s+/, $ChildCR;
	foreach my $ChildCR (@ChildCR)
	{
		chomp $ChildCR;
		my $CQEntity;
		eval {$CQEntity = $FFSession->GetEntity("Defect", $ChildCR)};
		if($@)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: unable to retrieve CQ entity '$ChildCR': $@\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: unable to retrieve CQ entity '$ChildCR': $@\n";
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}
		# Get CQ field value "Project"
		my $Project = &GetCQFieldValue($CQEntity, "Project");
		# Get CQ field value "Headline"
		my $Headline = &GetCQFieldValue($CQEntity, "Headline");
		if($Project eq $rules_downstream{'Project'} and $Headline =~ m/\A\s*\Q$FFPrefix\E/i)
		{
			return 1;
		}
	}
	return 0;
}


sub SetDynamicFieldValue
{
	my ($CQEntity, $CQfield) = @_;
	my ($choices, $res);
	eval {$choices = $CQEntity->GetFieldChoiceList($CQfield)};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'GetFieldChoiceList' ($CQfield) raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'GetFieldChoiceList' ($CQfield) raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	if(! scalar @$choices)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: CQ field '$CQfield' does not have choice list\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: CQ field '$CQfield' does not have choice list\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	# Check whether CQ field value is valid
	foreach my $choice (@$choices)
	{
		chomp $choice;
		if($choice eq $CQFieldsHash{$CQfield})
		{
			eval {$res = $CQEntity->SetFieldValue($CQfield, $CQFieldsHash{$CQfield})};
			if($@)
			{
				print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'SetFieldValue' ($CQfield) raised exceptions: $@\n";
				print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'SetFieldValue' ($CQfield) raised exceptions: $@\n";
				&UnbuildCQSession;
				&PostActions;
				exit(1);
			}
			if($res)
			{
				print FH_LOG   "Line: " . __LINE__ . " Error: unable to SetFieldValue '$CQfield': $res\n";
				print FH_ERROR "Line: " . __LINE__ . " Error: unable to SetFieldValue '$CQfield': $res\n";
				&UnbuildCQSession;
				&PostActions;
				exit(1);
			}
			return 1;
		}
	}
	# CQ field value is NOT valid, pick one as default at random
	eval {$res = $CQEntity->SetFieldValue($CQfield, @$choices[0])};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'SetFieldValue' ($CQfield) raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'SetFieldValue' ($CQfield) raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	if($res)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to SetFieldValue '$CQfield': $res\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to SetFieldValue '$CQfield': $res\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
}


sub ValidateAndCommit
{
	my ($CQEntity) = @_;
	my $res;
	eval {$res = $CQEntity->Validate()};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'Validate' raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'Validate' raised exceptions: $@\n";
		$CQEntity->Revert();
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	if($res)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'Validate' failed: $res\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'Validate' failed: $res\n";
		$CQEntity->Revert();
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	eval {$res = $CQEntity->Commit()};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'Commit' raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'Commit' raised exceptions: $@\n";
		$CQEntity->Revert();
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	if($res)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'Commit' failed: $res\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'Commit' failed: $res\n";
		$CQEntity->Revert();
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
}


# Eventually ...
sub ForwardFit
{
	my ($CQEntity) = @_;
	# Create a new CQ in downstream project
	my $res;
	my $FFCQEntity;
	eval {$FFCQEntity = $FFSession->BuildEntity("Defect")};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'BuildEntity' raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'BuildEntity' raised exceptions: $@\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	# Pre-define certain {FF}ed CQ field values
	if(! ($CQFieldsHash{'Headline'} =~ m/\A\s*\Q$FFPrefix\E/i))
	{
		$CQFieldsHash{'Headline'}  = $FFPrefix . $CQFieldsHash{'Headline'};
	}
	$CQFieldsHash{'RequestBy'} = "Forward_Fit";
	$CQFieldsHash{'NextVersion'} = "Forward_Fit";
	foreach my $rule (keys %rules_downstream)
	{
		chomp $rule;
		# Keep original CQ's State
		next if $rule eq "State";
		$CQFieldsHash{$rule} = $rules_downstream{$rule};
	}
	# Populate {FF}ed CQ field values with @required_submit_{static|dynamic}
	my @required_submit_static = qw(Project Headline Description Priority Severity CRType CRProperty 
		                            DiscoverPhase ReqID RequestBy Milestone);
	foreach my $CQfield (@required_submit_static)
	{
		chomp $CQfield;
		eval {$res = $FFCQEntity->SetFieldValue($CQfield, $CQFieldsHash{$CQfield})};
		if($@)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'SetFieldValue' ($CQfield) raised exceptions: $@\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'SetFieldValue' ($CQfield) raised exceptions: $@\n";
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}
		if($res)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: unable to SetFieldValue '$CQfield': $res\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: unable to SetFieldValue '$CQfield': $res\n";
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}		
	}
	my @required_submit_dynamic = qw(Owner BaseVersion Detected_Env Module dtype);
	foreach my $CQfield (@required_submit_dynamic)
	{
		chomp $CQfield;
		&SetDynamicFieldValue($FFCQEntity, $CQfield);
	}
	# Validate & Commit on success, Revert otherwise
	&ValidateAndCommit($FFCQEntity);

	# Check whether 'State' transition is required
	if(exists $rules_downstream{'State'} and $rules_downstream{'State'} =~ m/Assigned/i)
	{
		eval {$FFSession->EditEntity($FFCQEntity, "assign")};
		if($@)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'EditEntity' (assign) raised exceptions: $@\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'EditEntity' (assign) raised exceptions: $@\n";
			$FFCQEntity->Revert();
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}
		# Populate {FF}ed CQ field values with @required_assign_{static|dynamic}
		my @required_assign_static = qw(NextVersion FinishDate EstimateEffort);
		foreach my $CQfield (@required_assign_static)
		{
			chomp $CQfield;
			eval {$res = $FFCQEntity->SetFieldValue($CQfield, $CQFieldsHash{$CQfield})};
			if($@)
			{
				print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'SetFieldValue' ($CQfield) raised exceptions: $@\n";
				print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'SetFieldValue' ($CQfield) raised exceptions: $@\n";
				$FFCQEntity->Revert();
				&UnbuildCQSession;
				&PostActions;
				exit(1);
			}
			if($res)
			{
				print FH_LOG   "Line: " . __LINE__ . " Error: unable to SetFieldValue '$CQfield': $res\n";
				print FH_ERROR "Line: " . __LINE__ . " Error: unable to SetFieldValue '$CQfield': $res\n";
				$FFCQEntity->Revert();
				&UnbuildCQSession;
				&PostActions;
				exit(1);
			}		
		}
		my @required_assign_dynamic = qw(Owner ReviewPeer Verifier);
		foreach my $CQfield (@required_assign_dynamic)
		{
			chomp $CQfield;
			&SetDynamicFieldValue($FFCQEntity, $CQfield);
		}
		# Validate & Commit
		&ValidateAndCommit($FFCQEntity);
	}

	# Get {FF}ed CQ id
	my $FFID = &GetCQFieldValue($FFCQEntity, 'id');
	if(! $FFID)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: {FF}ed CQ ID is mandatory\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: {FF}ed CQ ID is mandatory\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	# Add a child link in the original CQ
	eval {$FFSession->EditEntity($CQEntity, "modify")};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'EditEntity' (modify) raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'EditEntity' (modify) raised exceptions: $@\n";
		$CQEntity->Revert();
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	eval {$res = $CQEntity->AddFieldValue("ChildCR", $FFID)};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'AddFieldValue' (ChildCR) raised exceptions: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'AddFieldValue' (ChildCR) raised exceptions: $@\n";
		$CQEntity->Revert();
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	if($res)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to AddFieldValue 'ChildCR': $res\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to AddFieldValue 'ChildCR': $res\n";
		$CQEntity->Revert();
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	# Check whether original CQ's State is 'Validated'
	if($CQFieldsHash{'State'} =~ m/\A\s*Validated\s*\Z/i)
	{
		# Set value for mandatory CQ field 'Note_Entry'
		eval {$res = $CQEntity->SetFieldValue("Note_Entry", "Updated by ForwardFit (add child link)")};
		if($@)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: Executing 'SetFieldValue' (Note_Entry) raised exceptions: $@\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: Executing 'SetFieldValue' (Note_Entry) raised exceptions: $@\n";
			$CQEntity->Revert();
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}
		if($res)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: unable to SetFieldValue 'Note_Entry': $res\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: unable to SetFieldValue 'Note_Entry': $res\n";
			$CQEntity->Revert();
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}	
	}
	# Validate & Commit
	&ValidateAndCommit($CQEntity);

	return $FFID;
}


sub FFManager
{
	my ($ffRuleFile) = @_;

	print FH_LOG "==> Querying CQ candidates and output to CSV\n";
	my $FFQueryResCount = &GetFFCandidates($ffRuleFile);
	print FH_LOG "==> CQ query returned '$FFQueryResCount' records\n\n";

	if(! open FH_CSV, "<FFlogs/$logDir/FFQuery_$ffRuleFile.csv")
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to open CSV file 'FFQuery_$ffRuleFile.csv': $!\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to open CSV file 'FFQuery_$ffRuleFile.csv': $!\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	if(! open FH_FFY, ">FFlogs/$logDir/FFY_$ffRuleFile.txt")
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to open file 'FFY_$ffRuleFile.txt': $!\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to open file 'FFY_$ffRuleFile.txt': $!\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	if(! open FH_FFN, ">FFlogs/$logDir/FFN_$ffRuleFile.txt")
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to open file 'FFN_$ffRuleFile.txt': $!\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to open file 'FFN_$ffRuleFile.txt': $!\n";
		&UnbuildCQSession;
		&PostActions;
		exit(1);
	}
	while(my $line = <FH_CSV>)
	{
		chomp $line;
		&InitCQFieldsHash;
		my @tmp = split /,/, $line;
		foreach my $CQfield (sort keys %CQFieldsHash)
		{
			chomp $CQfield;
			next if $CQfield eq "Description";
			if(defined $tmp[0] and $tmp[0] ne "")
			{
				$CQFieldsHash{$CQfield} = $tmp[0];
			}
			shift @tmp;
		}
		my $CQID = $CQFieldsHash{'id'};
		if(! $CQID)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: CQ ID is mandatory\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: CQ ID is mandatory\n";
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}
		my $CQEntity;
		eval {$CQEntity = $FFSession->GetEntity("Defect", $CQID)};
		if($@)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: unable to retrieve CQ entity '$CQID': $@\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: unable to retrieve CQ entity '$CQID': $@\n";
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}
		# Get CQ field value "Description"
		# print FH_LOG "==> Retrieve CQ field value 'Description' for '$CQID'\n";
		my $Description = &GetCQFieldValue($CQEntity, "Description");
		if($Description)
		{
			$CQFieldsHash{'Description'} = $Description;
		}
		# Get CQ field value "ChildCR"
		# print FH_LOG "==> Retrieve CQ field value 'ChildCR' for '$CQID'\n";
		my $ChildCR = &GetCQFieldValue($CQEntity, "ChildCR");

		# Skip if CQ has been {FF}ed
		print FH_LOG "==> Check whether CQ:$CQID has been {FF}ed: ";
		if(&HasBeenFFed($ChildCR))
		{
			print FH_LOG "==> Yes\n\n";
			print FH_FFN "==> CQ:$CQID has been {FF}ed before\n";
			next;
		}
		print FH_LOG "==> No\n\n";

		# Forward Fit
		print FH_LOG "==> Start Forward Fit for CQ:$CQID\n";
		my $FFID = &ForwardFit($CQEntity);
		if(! $FFID)
		{
			print FH_LOG   "Line: " . __LINE__ . " Error: {FF}ed CQ ID is mandatory\n";
			print FH_ERROR "Line: " . __LINE__ . " Error: {FF}ed CQ ID is mandatory\n";
			&UnbuildCQSession;
			&PostActions;
			exit(1);
		}
		print FH_LOG "==> Forward Fit Completed: $CQID {FF}ed to $FFID\n\n";
		print FH_FFY "==> Forward Fit Completed: $CQID {FF}ed to $FFID\n";
	}
	close FH_CSV;
	close FH_FFY;
	close FH_FFN;
}


sub UnbuildCQSession
{
	eval {CQSession::Unbuild($FFSession)};
	if($@)
	{
		print FH_LOG   "Line: " . __LINE__ . " Error: unable to Unbuild CQ session: $@\n";
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to Unbuild CQ session: $@\n";
		&PostActions;
		exit(1);
	}
}


# Archive logs & Send notification email
sub PostActions
{
	close FH_LOG;
	close FH_ERROR;
	if($logDir and -d "FFlogs/$logDir")
	{
		File::Copy::copy "ff_log.txt", "FFlogs/$logDir";
		File::Copy::copy "ff_errorlog.txt", "FFlogs/$logDir";
	}

	if(-s "ff_errorlog.txt")
	{
		# &Send_Mail_Failure;
	}
	else
	{
		# &Send_Mail_Success;
	}
}
