declaracionPrevia# char letrica
char1 = '5'
letrica = char1
declaracion# int _arr_[20]
declaracionPrevia# int _i_
int2 = 5
_i_ = int2
declaracionPrevia# int arr[3]
int3 = 22
int4 = 10
int5 = 11
arr[0] = int3
arr[1] = int4
arr[2] = int5
goto void$principal

int$f:
int6 = 1
return int6
int$f$final

bool$_funcionInt_:
declaracionPrevia# bool result
bool7 = true
int8 = 12
int9 = 34
int10 = 35
int11 = int9 + int10
bool12 = int8 >= int11
bool13 = not bool12
bool14 = bool7 and bool13
result = bool14

_funcionInt_$while$1$condicion:
bool15 = true
if bool15 goto _funcionInt_$while$1$bloque
goto _funcionInt_$while$1$final

_funcionInt_$while$1$bloque:
int16 = _i_
write int16
goto _funcionInt_$while$1$final
goto _funcionInt_$while$1$condicion

_funcionInt_$while$1$final:
bool17 = true
return bool17
bool$_funcionInt_$final

bool$_func2_:
param_bool _b1_
param_int _i1_
declaracionPrevia# bool x
int18 = 22
int19 = 45
bool20 = int18 < int19
x = bool20
bool21 = x
if bool21 goto _func2_$if$2$bloque1
goto _func2_$if$2$siguiente1

_func2_$if$2$bloque1:
declaracion# int y

_func2_$if$2$siguiente1:

_func2_$if$2$final:
bool22 = x
return bool22
bool$_func2_$final

void$principal:
declaracionPrevia# int var
int23 = 10
var = int23
int24 = 22
int25 = 45
bool26 = int24 < int25
int27 = var
int28 = 5
int29 = 6
int30 = int28 // int29
bool31 = int27 > int30
bool32 = bool26 and bool31
if bool32 goto principal$if$3$bloque1
goto principal$if$3$siguiente1

principal$if$3$bloque1:
declaracion# int y
declaracionPrevia# char ch33
char33 = 'a'
ch33 = char33
goto principal$if$3$final

principal$if$3$siguiente1:
bool34 = true
bool35 = false
bool36 = bool34 and bool35
bool37 = true
bool38 = bool36 or bool37
if bool38 goto principal$if$3$bloque2
goto principal$if$3$siguiente2

principal$if$3$bloque2:
declaracion# int y
goto principal$if$3$final

principal$if$3$siguiente2:
declaracion# int y
declaracionPrevia# string str2
string39 = "sdff"
str2 = string39

principal$if$3$final:
declaracion# int x
declaracionPrevia# int edades[3]
int40 = 22
int41 = 10
int42 = 11
edades[0] = int40
edades[1] = int41
edades[2] = int42

principal$for$4$inicializacion:
int43 = 0
x = int43

principal$for$4$condicion:
goto principal$for$4$bloque

principal$for$4$incremento:
int44 = x
int45 = int44 + 1
x = int45
goto principal$for$4$condicion

principal$for$4$bloque:
string46 = "Hola mundo"
write string46
int47 = x
int48 = 10
bool49 = int47 > int48
if bool49 goto principal$if$5$bloque1
goto principal$if$5$siguiente1

principal$if$5$bloque1:
int50 = edades[2]
int51 = 1
int52 = int50 + int51
edades[2] = int52
goto principal$for$4$final

principal$if$5$siguiente1:

principal$if$5$final:
goto principal$for$4$incremento

principal$for$4$final:
declaracion# int filas[5]
read filas[0]
int53 = 3
int54 = _i_
int55 = int53 + int54
int56 = -1
int57 = 5
int58 = int56 + int57
_arr_[int55] = int58
bool59 = true
int60 = 5
param bool59
param int60
bool61 = call bool$_func2_, 2
string62 = "fin del programa"
write string62
return
void$principal$final
