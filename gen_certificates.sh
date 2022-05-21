rm -f *.jks
rm -f certs/*

export AUTHOR="57473_58288"
export PASS="123456"
CACERTS_PATH="/usr/lib/jvm/java-8-openjdk/jre/lib/security/cacerts"

gen_certs_for() {
echo
echo
echo Generating for $1 with password $PASS$1
echo
echo

keytool -genkey -alias $1 -keyalg RSA -validity 365 -keystore ./$1.jks -storetype pkcs12 << EOF
$PASS$1
$PASS$1
$AUTHOR
TP2
SD2021
LX
LX
PT
yes
$PASS$1
$PASS$1
EOF


echo
echo
echo "Exporting Certificates"
echo
echo

keytool -exportcert -alias $1 -keystore $1.jks -file certs/$1.cert << EOF
$PASS$1
EOF

echo
echo "Adding certificate to Client Truststore"
echo
keytool -importcert -file certs/$1.cert -alias $1 -keystore client-ts.jks << EOF
changeit
yes
EOF
}

echo "Creating Client Truststore"
cp $CACERTS_PATH client-ts.jks

gen_certs_for users
gen_certs_for directory
gen_certs_for files