REM usage run.bat <code_dir>
REM add -javaagent:FULL\PATH\TO\jcov.jar=grabber -Xms512m -Xmx1024m to surefire argline
call java -jar JCOV_BUILD\jcov_3.0\jcov.jar tmplgen -verbose -t template.xml %1
start  cmd /c "java -jar JCOV_BUILD\jcov_3.0\jcov.jar grabber -vv -t template.xml -o result.xml > grabber_out.txt 2>&1"
pushd %1
call mvn install -Djacoco.skip=true -fn > install_out.txt 2>&1
popd
call java -jar JCOV_BUILD\jcov_3.0\jcov.jar grabberManager -save
call java -jar JCOV_BUILD\jcov_3.0\jcov.jar grabberManager -stop
call java -jar JCOV_BUILD\jcov_3.0\jcov.jar repgen -format text -o report  -src %1 -javap %1  -verbose -testsinfo result.xml 
call java -jar JCOV_BUILD\jcov_3.0\jcov.jar repgen -format html -o report_html  -src %1 -javap %1  -verbose -testsinfo result.xml 