#!/bin/bash

template="#00a2ff" # blue color that is used in template images do not change!

path_normal=$template
path_fire="#f6ff07"

rocket_normal=$path_normal

mkdir generated

for i in 0 1 2 3 4
do
	convert templates/cell${i}.png  -fill $path_fire -opaque $template generated/cell_${i}_fire.png
	convert templates/cell${i}.png  -fill $path_normal -opaque $template generated/cell_${i}.png
done

convert templates/rocket.png  -fill $rocket_normal -opaque $template generated/rocket.png
convert templates/rocket_fire.png  -fill $rocket_normal -opaque $template generated/rocket_fire.png
convert templates/shuffle.png  -fill $path_normal -opaque $template generated/shuffle.png
