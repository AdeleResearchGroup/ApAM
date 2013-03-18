set xrange[0:500]
#set yrange[0:1000]
unset output

set terminal "png" #epslatex  eepic  postscript pdfcairo dxf svg
set title "Apam performance measurement"
set ylabel "time (ms)"
set xlabel "nb of calls"

#plot \
#	'apamdata' using 5:4 title 'apam' with lines, \
#	'ariesdata' using 5:4 title 'aries' with lines, \
#	'ipojodata' using 5:4 title 'ipojo' with lines, \
#	'springdata' using 5:4 title 'spring' with lines, \
#	'tuscanydata' using 5:4 title 'tuscany' with lines \

plot 'apamdatainst' using 5:4 title 'apam' with lines, 'tuscanydatainst' using 5:4 title 'tuscany' with lines 



