# AES-GCM-Simple-Implementation

This is a FIPS Compliant implementation of the AES-GCM 256. To use it run the jar or use the gradle wrapper, providing the in the below format. 

    ./gradlew run --args "<mode(enc|dec)> <file_path> <output_file_name>"

For example, 

> ./gradlew run --args "enc 'c:/users/user/documents/my_doc.txt'
> my_enc_doc.enc 'w0rkWeek 31s sae3 2343@t___South' 1ssa22"

Will output the arbitrarily named encrypted variant, *my_enc_doc*, in the same directory *my_doc.txt* is located at.
