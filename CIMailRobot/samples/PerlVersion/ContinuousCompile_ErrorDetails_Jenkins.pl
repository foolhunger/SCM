#!/usr/bin/perl

use strict;
use warnings;

use File::Copy();


if( !open FH_ERROR, ">error.txt")
{
	print "Line: " . __LINE__ . " Error: unable to open file 'error.txt': $!\n";
	close FH_ERROR;
	Send_System_Error_Mail();
	exit(1);
}

if(scalar @ARGV != 6)
{
	print FH_ERROR "Line: " . __LINE__ . " Error: The amount of parameters passed in is not correct\n";
	exit(1);
}
my $project_name = $ARGV[0];
my $build_type = $ARGV[1];
my $build_time = $ARGV[2];
my $vob_location = $ARGV[3];
my $error_log_location = $ARGV[4];
my $email_receivers = $ARGV[5];

$vob_location =~ s/\\/\//g;
$error_log_location =~ s/\\/\//g;

print "==> 1: $project_name\n";
print "==> 2: $build_type\n";
print "==> 3: $build_time\n";
print "==> 4: $vob_location\n";
print "==> 5: $error_log_location\n";
print "==> 6: $email_receivers\n\n";

if( ! -d "$vob_location")
{
	print FH_ERROR "Line: " . __LINE__ . " Error: VOB location '$vob_location' does not exist: $!\n";
	close FH_ERROR;
	Send_System_Error_Mail();
	exit(1);
}

if( !open FH_MAIL_TEMPLATE, "email_template.html")
{
	print FH_ERROR "Line: " . __LINE__ . " Error: unable to open file 'email_template.html': $!\n";
	close FH_ERROR;
	Send_System_Error_Mail();
	exit(1);
}

if( !open FH_MAIL_BODY_HTML, ">email_body.html")
{
	print FH_ERROR "Line: " . __LINE__ . " Error: unable to open file 'email_body.html': $!\n";
	close FH_ERROR;
	Send_System_Error_Mail();
	exit(1);
}

if( !open FH_MAIL_XML, ">send_mail.xml")
{
	print FH_ERROR "Line: " . __LINE__ . " Error: unable to open file 'send_mail.xml': $!\n";
	close FH_ERROR;
	Send_System_Error_Mail();
	exit(1);
}

print "==> Processing Jenkins Error Log from folder $error_log_location\n";
if( !File::Copy::copy("$error_log_location/error.log", "./error.log"))
{
	print FH_ERROR "Line: " . __LINE__ . " Error: unable to copy Jenkins error log file to current folder: $!\n";
	close FH_ERROR;
	Send_System_Error_Mail();
	exit(1);
}

if( !open FH_LOG, "error.log")
{
	print FH_ERROR "Line: " . __LINE__ . " Error: unable to open Jenkins error log file 'error.log': $!\n";
	close FH_ERROR;
	Send_System_Error_Mail();
	exit(1);
}

my @tmp = ();
my $cmd = "";
# $error_flag indicates whether compilation failed
my $error_flag = "false";
while(<FH_LOG>)
{
	chomp;
	#   [javac] Compiling 527 source files to
	if($_ =~ m/\[javac\]\s*Compiling\s*\d+/i)
	{
		@tmp = ();
	}
	else
	{
		push @tmp, $_;
		# [java]     [javac] 3 errors
		# [java]     [javac] 11 errors
		# [java] BUILD FAILED
		if(($_ =~ m/\[javac\]\s*\d+\s*error/i) || ($_ =~ m/\[java\]\s*BUILD\s*FAILED\s*\Z/i))
		{
			$error_flag = "true";
			last;
		}
	}
}

my %hash = ();
my %owner_list = ();
my $init_flag = "false";
if($error_flag eq "false")
{
	print "==> No errors have been found in this build\n";
	exit(0);
}
else
{
	my $source_vob = "";
	if($vob_location =~ m/\A.+\/(.+?)\s*\Z/i)
	{
		$source_vob = $1;
		print "==> Source VOB: $source_vob\n";
	}
	else
	{
		print FH_ERROR "Line: " . __LINE__ . " Error: unable to locate source VOB\n";
		close FH_ERROR;
		Send_System_Error_Mail();
		exit(1);
	}

	foreach (@tmp)
	{
		# [javac] D:\cc_dailybuild\Chartis_BR_P25_Dev_GS_autodeploy\Chartis_BR_SRC\integration\cust\java\com\ebao\chartis\gs\integration\intf\uploadingci\business\cancellation\service\impl\CtsUpdCiEndorsementEntryServiceImpl.java:255: cannot find symbol
		# [javac] D:\cc_dailybuild\Nightbuild1_Chartis_BR_Phase25_GC_Dev_ContinueCompile\ChartisBR_GC_SRC\module\eclaim\cust\java\com\ebao\chartis\eclaim\claim\dao\hibernate\ChartisTClmCaseDaoHibernate.java:14: cannot find symbol
		# [javac] D:\cc_dailybuild\Nightbuild1_Chartis_BR_Phase25_GS_Dev_ContinueCompile\Chartis_BR_SRC\newbiz\cust\java\com\ebao\chartis\gs\pol\nb\operationstep\lottery\handler\ChartisLotteryManualTrigHandler.java:23: class ChartisLotterySinTrigHandler is public, should be declared in a file named ChartisLotterySinTrigHandler.java
		if($_ =~ m/\[javac\]\s*[A-Z]:\\.*\\\Q$source_vob\E\\(.+?\.java)/i)
		{
			$hash{$1} = "Error occurred while trying to retrieve detailed information";
		}
	}

	my $errorfile = "";
	foreach (sort keys %hash)
	{
		chomp;
		$errorfile = $_;
		if( -f "$vob_location/$errorfile")
		{
			print "##### $vob_location/$errorfile\n";
			my $real_filepath = "$vob_location/$errorfile";
			my $cmd = "cleartool desc -l \"$real_filepath\" >desc.txt";
			system($cmd);
			if( !open FH_DESC, "desc.txt")
			{
				print FH_ERROR "Line: " . __LINE__ . " Error: unable to open file 'desc.txt': $!\n";
				close FH_ERROR;
				Send_System_Error_Mail();
				exit(1);
			}
			while(<FH_DESC>)
			{
				chomp;
				#   created 18-Apr-11.15:42:14 by Bruce Peng (bruce.peng.GSPC_CCU@SHVM00934)
				#   created 07-六月-12.11:32:17 by xiang.chen.GSPC_CCU@SH02010880
				#   created 14-六月-12.20:16:27 by build_admin.Chartis_BR_CCU@CM-Build04
				#   created 14-六月-12.20:16:27 by cmadmin.Chartis_BR_CCU@CM-Build04
				#   created 14-六月-12.20:16:27 by income.deploy.01.Chartis_BR_CCU@CM-Build04
				if($_ =~ m/\A\s*created.*by\s*([a-zA-Z]+)([ \t\._])([a-zA-Z_]+)(\.\d+)?/i)
				{
					my ($t1, $t2, $t3, $t4) = ($1, $2, $3, "");
					$t4 = $4 if $4;
					if(!($3 =~ m/_CCU\s*\Z/i))
					{
						$hash{$errorfile} = "[<b style=\"color:#A52A2A\">$t1$t2$t3$t4</b>]";
						$owner_list{"$t1$t2$t3$t4"} = "stub";
					}
					else
					{
						$hash{$errorfile} = "[<b style=\"color:#A52A2A\">$t1$t4</b>]";
						$owner_list{"$t1$t4"} = "stub";
					}
				}
				#     activity:CRDB00563613@\GS_BL_PVOB  "deliver GS_GC_Dev on 4/18/2011 3:18:29 PM."
				if($_ =~ m/\A\s*activity:(.+)\@.*PVOB\s+(.+)\Z/i)
				{
					$hash{$errorfile} .= "[$1][$2]";
					$init_flag = "false";
				}
				if($_ =~ m/\A\s*\"created by clearfsimport\"\s*\Z/i)
				{
					$init_flag = "true";
				}
			}
		}
	}
	
	if($init_flag eq "true")
	{
		$hash{$errorfile} .= "[Initial import: created by clearfsimport]";
	}
}

close FH_ERROR;

if($build_type eq "0")
{
	$build_type="Continuous Compile";
}
if($build_type eq "1")
{
	$build_type="Daily Build";
}
$build_time =~ s/_/\@/;


# Generating HTML email body
my $naCount = 1;
while(<FH_MAIL_TEMPLATE>)
{
	chomp;
	if($_ =~ m/\A(\s*)\[__HEADLINE__\]\s*\Z/i)
	{
		if($error_flag eq "false")
		{
			print FH_MAIL_BODY_HTML "$1$build_type for $project_name compiled successfully\n";
		}
		if($error_flag eq "true")
		{
			print FH_MAIL_BODY_HTML "$1$build_type for $project_name encountered some compilation errors, Please refer to the details below\n";
		}
		next;
	}
	if(($error_flag eq "true") && ($_ =~ m/\A(\s*)(.+?)\[N\/A\](.+)\Z/i))
	{
		print FH_MAIL_BODY_HTML "$1$2\n";
		if($naCount == 1)
		{
			foreach (sort keys %hash)
			{
				print FH_MAIL_BODY_HTML "         file: $_<br>\n";
				print FH_MAIL_BODY_HTML "         detail: $hash{$_}<br><br>\n";
			}
		}
		if($naCount == 2)
		{
			foreach (@tmp)
			{
				# escape '<' and '>' to '(' and ')', otherwise they will mess up with HTML
				$_ =~ s/\</\(/g;
				$_ =~ s/\>/\)/g;
				print FH_MAIL_BODY_HTML "    $_<br>\n";
			}
		}
		print FH_MAIL_BODY_HTML "$1$3\n";
		$naCount = $naCount + 1;
		next;
	}
	print FH_MAIL_BODY_HTML "$_\n";
}

close FH_MAIL_BODY_HTML;


# Generating XML for sending email
print FH_MAIL_XML '<?xml version="1.0" encoding="GBK"?>', "\n";
print FH_MAIL_XML "\n";
print FH_MAIL_XML '<project name="send_mail" default="send" basedir=".">', "\n";
print FH_MAIL_XML "\n";
print FH_MAIL_XML '    <target name="send">', "\n";
print FH_MAIL_XML '	    <echo message="Sending notification mail to pre-defined receivers ..."/>', "\n";
my $status_title = "";
if($error_flag eq "true")
{
	$status_title = "Failure";
}
if($error_flag eq "false")
{
	$status_title = "Success";
}
print FH_MAIL_XML "	    <mail mailhost=\"mail.ebaotech.com\" subject=\"[$status_title]Detailed Information for $build_type $build_time\" messagemimetype=\"text/html\" messagefile=\"email_body.html\">", "\n";
print FH_MAIL_XML "		    <from name=\"[$project_name]$build_type Mailing Robot\" address=\"jackie.xiao\@ebaotech.com\"/>", "\n";
print FH_MAIL_XML "\n";
foreach (sort keys %owner_list)
{
	my $owner = $_;
	$owner =~ s/\A\s+//;
	$owner =~ s/\s+\Z//;
	$owner =~ s/\s+/\./;
	print FH_MAIL_XML "		    <to address=\"$owner\@ebaotech.com\"/>", "\n";
}
my @email_receivers = split ',', $email_receivers;
foreach (@email_receivers)
{
	chomp;
	print FH_MAIL_XML "		    <cc address=\"$_\"/>", "\n";
}
print FH_MAIL_XML "\n";
print FH_MAIL_XML "		    <bcc address=\"jackie.xiao\@ebaotech.com\"/>", "\n";
print FH_MAIL_XML "\n";
if ( -f "log")
{
	# print FH_MAIL_XML "			<fileset file=\"log\"/>", "\n";
}
if ( -s "error.txt")
{
	print FH_MAIL_XML "			<fileset file=\"error.txt\"/>", "\n";
}
print FH_MAIL_XML '	    </mail>', "\n";
print FH_MAIL_XML '	</target>', "\n";
print FH_MAIL_XML "\n";
print FH_MAIL_XML '</project>', "\n";

close FH_MAIL_XML;
	
$cmd = "call D:\\ant_1.7.1\\bin\\ant.bat -f send_mail.xml send >maillog.txt";
system($cmd);



sub Send_System_Error_Mail
{
	if( !open FH_SYSERR_MAIL, ">send_syserror_mail.xml")
	{
		print "Line: " . __LINE__ . " Error: unable to open file 'send_syserror_mail.xml': $!\n";
		exit(1);
	}
	
	$build_time =~ s/_/\@/;
	print FH_SYSERR_MAIL '<?xml version="1.0" encoding="GBK"?>', "\n";
	print FH_SYSERR_MAIL "\n";
	print FH_SYSERR_MAIL '<project name="send_syserror_mail" default="send" basedir=".">', "\n";
	print FH_SYSERR_MAIL "\n";
	print FH_SYSERR_MAIL '    <target name="send">', "\n";
	print FH_SYSERR_MAIL '	    <echo message="Sending notification mail to pre-defined receivers ..."/>', "\n";
	print FH_SYSERR_MAIL "	    <mail mailhost=\"mail.ebaotech.com\" subject=\"System Error(s) Occurred on $build_time (Jenkins)\">", "\n";
	print FH_SYSERR_MAIL "		    <from name=\"ContinuousCompile System Mailing Robot\" address=\"jackie.xiao\@ebaotech.com\"/>", "\n";
	print FH_SYSERR_MAIL "\n";
	print FH_SYSERR_MAIL "		    <to address=\"jackie.xiao\@ebaotech.com\"/>", "\n";
	print FH_SYSERR_MAIL "\n";
	if ( -f "error.txt")
	{
		print FH_SYSERR_MAIL "			<fileset file=\"error.txt\"/>", "\n";
	}
	print FH_SYSERR_MAIL '	    </mail>', "\n";
	print FH_SYSERR_MAIL '	</target>', "\n";
	print FH_SYSERR_MAIL "\n";
	print FH_SYSERR_MAIL '</project>', "\n";
	close FH_SYSERR_MAIL;
	
	$cmd = "call D:\\ant_1.7.1\\bin\\ant.bat -f send_syserror_mail.xml send >maillog.txt";
	system($cmd);
}
