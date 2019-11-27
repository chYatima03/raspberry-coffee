#!/usr/bin/env python3
#
# Read a CSV file generated by the Android app at
# https://github.com/OlivierLD/raspberry-coffee/tree/master/Raspberry.Droid/AstroComputer
# and transforms it into a KML file, compatible with Google Maps
#
# Usage is:
#   csv.to.kml.py --in:GPS_DATA.csv --out:somefile.kml --title:"Cool Logging" --sub-title:"Done today by myself"
#
# Then, in google maps, logged in:
# - Menu (hamburger)
# - Your places
# - MAPS (toolbar, right)
# - CREATE MAP (bottom)
# - Click "Import"
# - Drag your KML into the Upload zone
# That's it!
#
import sys

print("{} script arguments.".format(len(sys.argv) - 1))

if len(sys.argv) < 5:  # Script name + 4 args
    print("\nUsage is ")
    print(
        '\tpython {} --in:GPS_DATA.csv --out:somefile.kml --title:"Cool Logging" --sub-title:"Done today by myself"'.format(
            sys.argv[0]))
    print("Try again")
    sys.exit()

input_file = ""
output_file = ""

title = ""
sub_title = ""

for arg in sys.argv:
    if arg[:5] == '--in:':
        input_file = arg[5:]
    elif arg[:6] == '--out:':
        output_file = arg[6:]
    elif arg[:8] == '--title:':
        title = arg[8:]
    elif arg[:12] == '--sub-title:':
        sub_title = arg[12:]

if len(input_file) == 0 or len(output_file) == 0 or len(title) == 0 or len(sub_title) == 0:
    print("All parameters --in: --out: --title: and --sub-title: are required.")
    print("Try again")
    sys.exit()

print("Converting {} into {}".format(input_file, output_file))

data_in = open(input_file, "r")
data_out = open(output_file, "w")

kml_preamble_part_1 = "<?xml version = '1.0' encoding = 'UTF-8'?>\n" \
                      "<kml xmlns=\"http://earth.google.com/kml/2.0\" \n" \
                      "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" \
                      "     xsi:schemaLocation=\"http://earth.google.com/kml/2.0 ../xsd/kml21.xsd\">\n" \
                      "   <Document>\n" \
                      "      <name>{}</name>\n".format(title)

kml_preamble_part_2 = "      <Placemark>\n" \
                      "          <name>{}</name>\n" \
                      "          <visibility>1</visibility>\n" \
                      "          <open>0</open>\n" \
                      "          <Style>\n" \
                      "             <LineStyle>\n" \
                      "                <width>3</width>\n" \
                      "                <color>ff00ffff</color>\n" \
                      "             </LineStyle>\n" \
                      "             <PolyStyle>\n" \
                      "                <color>7f00ff00</color>\n" \
                      "             </PolyStyle>\n" \
                      "          </Style>\n" \
                      "          <LineString>\n" \
                      "             <extrude>1</extrude>\n" \
                      "             <tessellate>1</tessellate>\n" \
                      "             <altitudeMode>clampToGround</altitudeMode>\n" \
                      "             <coordinates>\n".format(sub_title)
data_out.write(kml_preamble_part_1)
data_out.write(kml_preamble_part_2)

latitude_index = -1
longitude_index = -1

for input_line in data_in:
    # print(input_line)
    data = input_line.split(';')
    if latitude_index == -1 and longitude_index == -1:  # Look for the indexes of interest
        for idx in range(len(data)):
            if data[idx] == 'latitude':
                latitude_index = idx
            if data[idx] == 'longitude':
                longitude_index = idx
            if latitude_index != -1 and longitude_index != -1:
                break
        print("Found latitude in column {}, longitude in column {}".format(latitude_index, longitude_index))
    else:  # Coordinates format is 'longitude,latitude,altitude' <= Careful: longitude goes first.
        output_line = "{},{},{}\n".format(data[longitude_index], data[latitude_index], 0)
        data_out.write(output_line)

# Closing tags
kml_closing_tags = "            </coordinates>\n" \
                   "         </LineString>\n" \
                   "      </Placemark>\n" \
                   "   </Document>\n" \
                   "</kml>\n"
data_out.write(kml_closing_tags)

data_in.close()
data_out.close()

print("Done")
print("{} ready for export".format(output_file))
