from tensorflow.keras.preprocessing.image import img_to_array
from tensorflow.keras.models import load_model
from tensorflow.keras import optimizers
import numpy as np
#import imutils
#import cv2


import os
import time
import boto3
import urllib.request
from config import Config
from flask_sqlalchemy import SQLAlchemy
from werkzeug.utils import secure_filename
from werkzeug.datastructures import  FileStorage
from flask import Flask, request, jsonify, render_template, redirect, flash, url_for, \
    Response
from tensorflow.keras.preprocessing.image import img_to_array, load_img


from tables import Results, ResultsAdmin
from sqlalchemy import func
from filters import datetimeformat, file_type
from flask_bootstrap import Bootstrap

from config import S3_BUCKET, S3_KEY, S3_SECRET

app = Flask(__name__)
Bootstrap(app)
app.secret_key = 'secret'
app.jinja_env.filters['datetimeformat'] = datetimeformat
app.jinja_env.filters['file_type'] = file_type


#S3_BUCKET='caninetensorflask-bucket'
#S3_KEY='fu2sYwfCqgl52vQianGSvdWEkFgAQq6UKLuCQlpq'
#S3_SECRET_ACCESS_KEY='AKIAINW5QQ64CAXLEY7Q'

POSTGRES = {
    'user': 'postgres',
    'pw': 'password',
    'db': 'my_database',
    'host': 'localhost',
    'port': '5432',
}




app.config.from_object(Config)
#app.config.from_object(os.environ['APP_SETTINGS'])
app.config['DEBUG'] = True
app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql://%(user)s:\
%(pw)s@%(host)s:%(port)s/%(db)s' % POSTGRES

app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)


#s3_resources = boto3.client(
#                                    "s3",
#                                    aws_access_key_id= S3_SECRET_ACCESS_KEY,
#                                    aws_secret_access_key= S3_KEY
#                                    )

#s3_resource = boto3.resource(
#   "s3",
#   aws_access_key_id=S3_KEY,
#   aws_secret_access_key=S3_SECRET
#)


S3_BUCKET = 'caninetensorflask-bucket'


s3_resources = boto3.client("s3")






from models import Canine




@app.route('/storage')
def files():
    s3_resource = boto3.resource('s3')
    my_bucket = s3_resource.Bucket(S3_BUCKET)
    summaries = my_bucket.objects.all()

    return render_template('files.html', my_bucket=my_bucket, files=summaries)


@app.route('/upload', methods=['POST'])
def upload():
    file = request.files['file']

    s3_resource = boto3.resource('s3')
    my_bucket = s3_resource.Bucket(S3_BUCKET)
    my_bucket.Object(file.filename).put(Body=file)

    flash('File uploaded successfully')
    return redirect(url_for('files'))

@app.route('/delete', methods=['POST'])
def delete():
    key = request.form['key']

    s3_resource = boto3.resource('s3')
    my_bucket = s3_resource.Bucket(S3_BUCKET)
    my_bucket.Object(key).delete()

    flash('File deleted successfully')
    return redirect(url_for('files'))

@app.route('/download', methods=['POST'])
def download():
    key = request.form['key']

    s3_resource = boto3.resource('s3')
    my_bucket = s3_resource.Bucket(S3_BUCKET)

    file_obj = my_bucket.Object(key).get()

    return Response(
        file_obj['Body'].read(),
        mimetype='text/plain',
        headers={"Content-Disposition": "attachment;filename={}".format(key)}
    )






@app.route('/hello')
def hello():
    return "DOG DOCTOR :)"



@app.route('/')
def index():
    return render_template('classify.html')


@app.route('/', methods=['POST', 'GET'])
def submit_file():
    if request.method == 'POST':
        if 'file' not in request.files:
            flash('No file part')
            return redirect(request.url)
        file = request.files['file']
        if file.filename == '':
            flash('No file selected for uploading')
            return redirect(request.url)
        if file:
            filename = secure_filename(file.filename)
            file.save(os.path.join(app.config['UPLOAD_FOLDER'],filename))
            label, confidence = tensor_classify(filename)
            breed=request.form.get('breed')
            sex=request.form.get('sex')
            agemonth=request.form.get('agemonth')
            toDatabaseWeb(label, confidence, breed, sex, agemonth, filename)
            toS3(filename, label, confidence)


            flash(label)
            flash(confidence)
            #flash(filename)

            return redirect('/')





@app.route('/postdata_tensor', methods = ['GET', 'POST'])
def app_classify():
    imagefile = request.files['image']
    filename = ''
    if imagefile:
        filename = secure_filename(imagefile.filename)
        imagefile.save(os.path.join('/tmp/',filename))
        label,confidence = tensor_classify(filename)
        
        toDatabase(filename, label, confidence)
        toS3(filename, label, confidence)
        toAndroid = "Disease: " + label + "\nConfidence: " + confidence

        return  toAndroid

    return
       




@app.route('/getalljson')
def get_all_json():
    try:
        canines=Canine.query.all()
        return  jsonify([e.serialize() for e in canines])
    except Exception as e:
	    return(str(e))


@app.route('/get/<id_>')
def get_by_id(id_):
    try:
        canine=Canine.query.filter_by(id=id_).first()
        return jsonify(canine.serialize())
    except Exception as e:
	    return(str(e))
    return




@app.route('/getall')
def get_all_admin():
 
    canines=Canine.query.all()

    table = ResultsAdmin(canines)
    table.border = True
    return render_template('results.html', table=table)



@app.route('/getalltest')
def get_all():
 
    canines=Canine.query.all()

    table = Results(canines)
    table.border = True
    return render_template('results.html', table=table)





@app.route('/deleteone/<id_>')
def delete_one(id_):
    try:
    
        obj = Canine.query.filter_by(id=id_).one()
        db.session.delete(obj)
        db.session.commit()
        returnstr = "id:" + id_ + " deleted :(" 
        return returnstr
    except Exception as e:
	    return(str(e))
    return


@app.route('/deleteall')
def delete_all():
        try:
            db.session.query(Canine).delete()
            db.session.commit()
            return "table wiped :("
        except Exception as e:
	        return(str(e))
        return




@app.route("/add",methods=['GET', 'POST'])
def add_form():
    if request.method == 'POST':
        Disease=request.form.get('disease')
        Confidence=request.form.get('confidence')
        Breed=request.form.get('breed')
        Sex=request.form.get('sex')
        AgeMonth=request.form.get('agemonth')
        Filename=request.form.get('filename')
        Time=request.form.get('time')

        try:
            canine=Canine(
                Disease=Disease,
                Confidence=Confidence,
                Breed=Breed,
                Sex=Sex,
                AgeMonth=AgeMonth,
                Filename = Filename,
                Time = Time
            )
            db.session.add(canine)
            db.session.commit()
            flash("Dog details added. Dog id={}".format(canine.id))
            return render_template("getdata.html")
            #return "Canine details added. Canine id={}".format(canine.id)
        except Exception as e:
	        return(str(e))
    return render_template("getdata.html")





def tensor_classify(filename):
    imagetest = load_img('/tmp/'+filename , target_size=(224, 224))
    image = img_to_array(imagetest)
    image = image.astype("float32") / 255.0
    image = np.expand_dims(image, axis=0)

    print("[INFO] loading network...")
    #model = load_model('/home/mcbtaguiad/Documents/aws_for_deploy/model_foronlinev2.h5')
    model = load_model('/home/ubuntu/model/model_foronlinev2.h5')


    print("[INFO] classifying image...")
    predicted_batch = model.predict_on_batch(image)
    predicted_batch = np.squeeze(predicted_batch)
    predicted_ids = np.argmax(predicted_batch, axis=-1)
    labelconf = predicted_batch[predicted_ids]*100
    labelconf = str(format(labelconf, '.4f')) + "%"

    if predicted_ids == 0: 
        labelstr = "Atresia Ani"
        
    if predicted_ids == 1: 
        labelstr = "Rectal Prolapse"

    if predicted_ids == 2: 
        labelstr = "Anal Sac Disease"

    
    print(labelstr)
    print(labelconf)



    
    return  labelstr, labelconf


def toDatabase(filename,label, confidence):

    valstr1 = filename.replace('.jpg','')
    valstr2 = valstr1.replace('_',' ')
    dogdata = valstr2.split('---')
    filestr= label+ '_' + confidence + '_' +  filename.replace('---','_')
    timestr = time.strftime("%B-%d-%Y %a %I:%M %p")

    Disease= label
    Confidence= confidence
    Breed=dogdata[0]
    Sex=dogdata[1]
    AgeMonth=dogdata[2]
    Filename = filestr
    Time = timestr

    try:
        canine=Canine(
            Disease=Disease,
            Confidence=Confidence,
            Breed=Breed,
            Sex=Sex,
            AgeMonth=AgeMonth,
            Filename = Filename,
            Time = Time
            )
        db.session.add(canine)
        db.session.commit()
            
        return "Canine details added. Canine id={}".format(canine.id)
    except Exception as e:
	    return(str(e))
    return



def toDatabaseWeb(label, confidence, breed, sex, agemonth, filename):

    #valstr1 = filename.replace('.jpg','')
    #valstr2 = valstr1.replace('_',' ')
    #dogdata = valstr2.split('---')
    filestr= label+ '_' + confidence + '_' +  filename
    timestr = time.strftime("%B-%d-%Y %a %I:%M %p")

    Disease= label
    Confidence= confidence
    Breed=breed
    Sex=sex
    AgeMonth=agemonth
    Filename = filestr
    Time = timestr

    try:
        canine=Canine(
            Disease=Disease,
            Confidence=Confidence,
            Breed=Breed,
            Sex=Sex,
            AgeMonth=AgeMonth,
            Filename = Filename,
            Time = Time
            )
        db.session.add(canine)
        db.session.commit()
            
        return "Canine details added. Canine id={}".format(canine.id)
    except Exception as e:
	    return(str(e))
    return




def toS3(filename, label, confidence):

    toS3 = label+ '_' + confidence + '_' +  filename.replace('---','_')

    s3_upload = boto3.client(
                                    "s3",
                                    aws_access_key_id= app.config['S3_SECRET_ACCESS_KEY'],
                                    aws_secret_access_key= app.config['S3_KEY']
                                    )

    bucket_resource = s3_upload

    try:
        file_path = '/tmp/' + filename
        bucket_resource.upload_file(
                                                            Bucket = app.config['S3_BUCKET'],
                                                            Filename=file_path,
                                                            Key=toS3
                                                            )
        
        return "Uploaded to S3"
    except Exception as e:
	    return(str(e))

    return
    



if __name__ == '__main__':
     app.run(threaded=True, port=5000)

    
#app.run(host="0.0.0.0")
