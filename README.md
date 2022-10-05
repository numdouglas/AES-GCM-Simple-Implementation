# AES-GCM-Simple-Implementation

This is a FIPS Compliant implementation of the AES-GCM 256. To use it, run the jar (in build/libs) or the gradle wrapper, providing the *\<arguments\>* in the below format. 

    ./gradlew run --args "<mode(enc|dec)> <file_path> <output_file_name>"

And Enter a password on prompt.

For example, 

> ./gradlew run --args "enc 'c:/users/user/documents/my_doc.txt'
> my_enc_doc.enc"

Will output the arbitrarily named encrypted variant, *my_enc_doc*, in the same directory *my_doc.txt* is located.
