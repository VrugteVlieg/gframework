grammar dyck;
d : ( LBRACK d RBRACK  d) | ;
LBRACK : '[';
RBRACK : ']';