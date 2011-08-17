#Sample keytool commands to generate two self-signed certificate and export them into a trust store.

echo changeit > keystore.pin

keytool -genkey -alias openidm-local-openidm-forgerock-org -keyalg rsa -dname "CN=local.openidm.forgerock.org, O=OpenIDM Self-Signed Certificate" -keystore keystore.jceks -storetype JCEKS
keytool -genkey -alias openidm-localhost -keyalg rsa -dname "CN=localhost, O=OpenIDM Self-Signed Certificate" -keystore keystore.jceks -storetype JCEKS

keytool -selfcert -alias openidm-local-openidm-forgerock-org -validity 3653 -keystore keystore.jceks -storetype JCEKS
keytool -selfcert -alias openidm-localhost -validity 3653 -keystore keystore.jceks -storetype JCEKS


keytool -export -alias openidm-local-openidm-forgerock-org -file openidm-local-openidm-forgerock-org-cert.txt -rfc -keystore keystore.jceks -storetype JCEKS
keytool -export -alias openidm-localhost -file openidm-localhost-cert.txt -rfc -keystore keystore.jceks -storetype JCEKS


keytool -import -alias openidm-local-openidm-forgerock-org -file openidm-local-openidm-forgerock-org-cert.txt -keystore truststore -storetype JKS
keytool -import -alias openidm-localhost -file openidm-localhost-cert.txt -keystore truststore -storetype JKS