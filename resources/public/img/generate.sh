#!/bin/bash

template="#00a2ff" # blue color that is used in template images do not change!

path_normal=$template
path_fire="#f6ff07"
selected_cell=$path_fire

rocket_normal=$path_normal
fuel=$path_fire

for i in 0 1 2 3 4
do
	convert templates/cell${i}.png  -fill $path_fire -opaque $template generated/cell_${i}_fire.png
	convert templates/cell${i}.png  -fill $path_normal -opaque $template generated/cell_${i}.png
done

convert templates/rocket.png  -fill $rocket_normal -opaque $template generated/rocket.png
convert templates/rocket_fire.png  -fill $rocket_normal -opaque $template generated/rocket_fire.png
convert templates/shuffle.png  -fill $path_normal -opaque $template generated/shuffle.png
convert templates/selected.png  -fill $selected_cell -opaque $template generated/selected.png

cp templates/empty_rocket.png generated/fuel_0.png
convert templates/empty_rocket.png -fill $path_fire -stroke $path_fire -draw "rectangle 9 23 26 49" generated/fuel_3.png
convert templates/empty_rocket.png -fill $path_fire -stroke $path_fire -draw "rectangle 9 32 26 49" generated/fuel_2.png
convert templates/empty_rocket.png -fill $path_fire -stroke $path_fire -draw "rectangle 9 41 26 49" generated/fuel_1.png