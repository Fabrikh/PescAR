# A Flask backend for AquARium

import base64, sys, os, random
from flask import Flask,jsonify, request
import csv

app = Flask(__name__)

ASSETS_FOLDER = "./mysite/assets"
MAX_CARD_RARITY = 3
BOOT_CHANCE_PERCENTAGE = 50
rarityValues = []
bootValues = [0 for i in range(0,100 - BOOT_CHANCE_PERCENTAGE)] + [1 for i in range(0,BOOT_CHANCE_PERCENTAGE)]

def probLaw(value):
    return MAX_CARD_RARITY + 1 - value


@app.before_first_request
def init():

    global rarityValues

    with open(os.path.join(ASSETS_FOLDER, 'data/fish.csv'), 'r') as file:

        reader = csv.DictReader(file)
        for row in reader:
            if int(row['id']) == 0:
                continue

            rarityValues += [ int(row['id']) for i in range(0,probLaw(int(row['rarity']))) ]
        print("Lista rarity ",rarityValues, file = sys.stderr)



@app.route('/')
def get_random_fish():

    id = int(request.args.get('id', '-1'))
    difficulty = int(request.args.get('difficulty', '0'))

    print(id,"random" if id == -1 else "specific", file = sys.stderr)

    if id == -1:
        if difficulty != 0:
            boot = random.choice(bootValues)
            if boot == 1:
                id = 0
            else:
                id = random.choice(rarityValues)
        else:
            id = random.choice(rarityValues)

    image_path = os.path.join(ASSETS_FOLDER, f'images/{id}.png')

    response = {}

    # Leggi l'immagine e convertila in base64
    with open(image_path, 'rb') as image_file:
        encoded_image = base64.b64encode(image_file.read()).decode('utf-8')

    with open(os.path.join(ASSETS_FOLDER, 'data/fish.csv'), 'r') as file:

        reader = csv.DictReader(file)
        for row in reader:
            if row['id'] == str(id):

                response = {
                    'id': row['id'],
                    'rarity': row['rarity'],
                    'name': row['nome'],
                    'description': row['descrizione'],
                    'image': encoded_image,
                    'format': 'jpeg'
                }

                break

    print("Hai pescato un "+ response['name'], file = sys.stderr)

    return jsonify(response)

@app.route('/fishcount')
def get_fish_count():

    return str(len(os.listdir(os.path.join(ASSETS_FOLDER, 'images'))))

@app.route('/fishgrid')
def get_fish_list():


    res = {}

    for id in range(len(os.listdir(os.path.join(ASSETS_FOLDER, 'images')))):

        image_path = os.path.join(ASSETS_FOLDER, f'images/{id}.png')

        with open(image_path, 'rb') as image_file:
            encoded_image = base64.b64encode(image_file.read()).decode('utf-8')

        res[str(id)] = encoded_image

    print("Inviata lista pesci", file = sys.stderr)

    return jsonify(res)


if __name__ == '__main__':
    app.run(port=5003)
