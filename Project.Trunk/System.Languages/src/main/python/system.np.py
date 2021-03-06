import numpy as np
import datetime
#
# Here we use the 'numpy' Python library
# Is simplifies the process quite a bit ;)
#
# See https://www.tutorialspoint.com/numpy/
# For special characters and symbols, see https://pythonforundergradengineers.com/unicode-characters-in-python.html
#
matrix = np.array([
    [12, 13, 14],
    [1.345, -654, 0.001],
    [23.09, 5.3, -12.34]
])

print("System coefficients:", matrix)

constants = np.array([234, 98.87, 9.876])
print("System constants", constants)

before = datetime.datetime.now()
solution = np.linalg.solve(matrix, constants)
after = datetime.datetime.now()
print("Done in {}s, {}\u03BCs".format((after - before).seconds, (after - before).microseconds))
print("By 'solve':", solution)

before = datetime.datetime.now()
# Resolution
inverse = np.linalg.inv(matrix)
print('Inverse:', inverse)
solution_2 = np.dot(inverse, constants)
after = datetime.datetime.now()
print("Done in {}s, {}\u03BCs".format((after - before).seconds, (after - before).microseconds))
print("By 'dot product':", solution_2)
