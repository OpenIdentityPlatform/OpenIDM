#!/usr/bin/perl
use strict;
use warnings;
use File::Find;
use File::Basename;
use File::Path qw/make_path/;

my $adminJsDirectoryPath = "../../../../openidm-ui-admin/src/main/js/org";
my $adminTestDirectoryPath = "../../../../openidm-ui-admin/src/test/qunit/";
my $commonJsDirectoryPath = "../../../../openidm-ui-common/src/main/js/org";
my $commonTestDirectoryPath = "../../../../openidm-ui-common/src/test/qunit/tests";
my $enduserJsDirectoryPath = "../../../../openidm-ui-enduser/src/main/js/org";
my $enduserTestDirectoryPath = "../../../../openidm-ui-enduser/src/test/qunit/";

sub process_files {
        my @files;
        my $jsDirectory = $_[0];
        my $testDirectory = $_[1];
        my $mainPath = "$testDirectory/main2.js";
        my $mainjsText = "define([\n";

        find(
            sub { push @files, $File::Find::name unless -d; },
            $jsDirectory
        );
        for my $file (@files) {
            ## get the path/name of the module
            my $moduleName = $file;
            my $testName = $file;

            $moduleName =~ s/$jsDirectory/org/g;
            $testName =~ s/$jsDirectory/org/g;
            $testName =~ s/\.js/Test/g;

            my $testPath = "$testDirectory/$testName.js";

            ##concatenate text for main.js
            $mainjsText = "$mainjsText    \"./$testName\",\n";

            ##check the test directory to see if the test file exists
            ##if no test exists create the test file
            unless (-f $testPath) {
                create_test_file($moduleName, $testPath);
            } else {
                print "$testPath--exists\n\n";
            }
        }

        $mainjsText = "$mainjsText]);";

        ##create main.js file
        open my $fh, '>', $mainPath or die "Ouch: $!\n";
        ##write to the file
        print $fh $mainjsText;
        ##close the file
        close $fh;
}

sub create_test_file {
    my $moduleName = $_[0];
    my $testPath = $_[1];
    my $dir = dirname($testPath);
    make_path($dir);

    print "$testPath--does not exist...creating\n\n";

    ##create the file
    open my $fh, '>', $testPath or die "Ouch: $!\n";
    ##write to the file
    write_test_file($moduleName, $testPath, $fh);
    ##close the file
    close $fh;
}

sub write_test_file {
    my $moduleName = $_[0];
    $moduleName =~ s/\.js//g;
    print "$moduleName\n";
    my $testPath = $_[1];
    my $fh = $_[2];
    my $moduleVariableName = basename($testPath);
    $moduleVariableName =~ s/Test\.js//g;

    my $testTemplate = "define([
    \"$moduleName\"
], function ($moduleVariableName) {
    QUnit.module('$moduleVariableName Tests');
});";

    print $fh $testTemplate;
}

process_files($enduserJsDirectoryPath, $enduserTestDirectoryPath);
