import os
import numpy as np
import pandas as pd
import tensorflow.keras
from sklearn.datasets import load_files
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Input
from tensorflow.keras import optimizers
from tensorflow.keras import applications
from tensorflow.keras.models import Sequential
from tensorflow.keras.callbacks import ModelCheckpoint
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.layers import GlobalAveragePooling2D,Dense,Flatten,Dropout,AveragePooling2D
import matplotlib.pyplot as plt
import matplotlib.pyplot as plt1
import matplotlib.pyplot as plt2
import matplotlib.pyplot as plt3
from tensorflow.keras.preprocessing.image import img_to_array, load_img
from tensorflow.keras import utils
from sklearn.model_selection import train_test_split
import math
from sklearn.metrics import classification_report
from sklearn.metrics import confusion_matrix
import itertools
 
data_dir = 'training_dir'
 
data = load_files(data_dir)
X = np.array(data['filenames'])
y = np.array(data['target'])
labels = np.array(data['target_names'])
 
 
pyc_file_pos = (np.where(file==X) for file in X if file.endswith(('.pyc','.py')))
for pos in pyc_file_pos:
   X = np.delete(X,pos)
   y = np.delete(y,pos)
 
def convert_img_to_arr(file_path_list):
   arr = []
   for file_path in file_path_list:
       img = load_img(file_path, target_size = (224,224))
       img = img_to_array(img)
       arr.append(img)
   return arr
 
X = np.array(convert_img_to_arr(X))
X = X.astype('float32')/255
 
no_of_classes = len(np.unique(y))
y = np.array(utils.to_categorical(y,no_of_classes))
 
X_train, X_test, y_train, y_test = train_test_split(X,y,test_size=0.4)
X_test,X_valid, y_test, y_valid = train_test_split(X_test,y_test, test_size = 0.4)
 
epochs = 100
batch_size = 2
target_size = 224
 
 
input_tensor = Input(shape=(target_size, target_size, 3))
baseModel = applications.MobileNetV2(
       include_top=False,
       weights='imagenet',
       input_tensor=input_tensor,
       input_shape=(target_size, target_size, 3),
       )
 
print('Loaded model!')
 
headModel = baseModel.output
headModel = GlobalAveragePooling2D(input_shape=baseModel.output_shape[1:])(headModel)
headModel = Dropout(0.75)(headModel)
headModel=Dense(1024,activation='relu')(headModel) headModel=Dense(1024,activation='relu')(headModel) headModel=Dense(512,activation='relu')(headModel) 
headModel=Dense(no_of_classes,activation='softmax')(headModel)
model = Model(inputs=baseModel.input, outputs=headModel)
 
model.summary()
 
for layer in baseModel.layers:
   layer.trainable = False
 
 
opt1 = optimizers.Adam(lr=1e-4)
opt2 = optimizers.SGD(lr=1e-4, momentum=0.9)
model.compile(loss='categorical_crossentropy',
             optimizer=opt2,
             metrics=['accuracy']
             )
 
 
train_datagen = ImageDataGenerator(
 rotation_range=20,
 zoom_range=0.15,
 width_shift_range=0.2,
 height_shift_range=0.2,
 shear_range=0.15,
 horizontal_flip=True,
 fill_mode="nearest")
 
 
test_datagen = ImageDataGenerator()
 
train_generator = train_datagen.flow(
   X_train,y_train,
   batch_size=batch_size,
   shuffle=True
   )
 
validation_generator = test_datagen.flow(
   X_valid,y_valid,
   batch_size=batch_size,
   shuffle=False
   )
 
history = model.fit(
   train_generator,
   steps_per_epoch=len(X_train) // batch_size,
   epochs = epochs ,
   validation_data=validation_generator,
   validation_steps=len(X_valid) // batch_size,
   )
 
 
(eval_loss, eval_accuracy) = model.evaluate( 
    X_test, y_test, batch_size=batch_size, verbose=1)
 
print("Accuracy: {:.4f}%".format(eval_accuracy * 100)) 
print("Loss: {}".format(eval_loss))
 
print("[INFO] evaluating network...")
predIdxs = model.predict(X_test, batch_size=batch_size)
 
predIdxs = np.argmax(predIdxs, axis=1)
 
print(classification_report(y_test.argmax(axis=1), predIdxs,
 target_names=['Atresia Ani','Rectal Prolapse','Anal Sac Disease']))
 
 
def plot_confusion_matrix(cm, classes,
                       normalize=True,
                       title='Confusion matrix',
                       cmap=plt.cm.Blues):
   """
   This function prints and plots the confusion matrix.
   Normalization can be applied by setting `normalize=True`.
   """
   plt.style.use("ggplot")
   plt.imshow(cm, interpolation='nearest', cmap=cmap)
   plt.title(title)
   plt.colorbar()
   tick_marks = np.arange(len(classes))
   plt.xticks(tick_marks, classes, rotation=45)
   plt.yticks(tick_marks, classes)
 
   if normalize:
       cm = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis]
       print("Normalized confusion matrix")
   else:
       print('Confusion matrix, without normalization')
 
   print(cm)
 
   fmt = '.2f' if normalize else 'd'
   thresh = cm.max() / 2.
   for i, j in itertools.product(range(cm.shape[0]), range(cm.shape[1])):
     plt.text(j, i, format(cm[i, j], fmt), horizontalalignment='center', color='white' if cm[i, j] > thresh else 'black')
   plt.tight_layout()
   plt.ylabel('True Class')
   plt.xlabel('Predicted Class')
   plt.show()
 
cm = confusion_matrix(y_test.argmax(axis=1),predIdxs)
print(cm)
cm_plot_labels = ['Atresia Ani','Rectal Prolapse','Anal Sac Disease']
plot_confusion_matrix(cm=cm, classes=cm_plot_labels, title='Confusion Matrix')
 
 
def plot(history):
   plt3.figure(1)
   plt3.subplot(211) 
   plt3.plot(history.history['accuracy']) 
   plt3.plot(history.history['val_accuracy']) 
   plt3.title('model accuracy') 
   plt3.ylabel('accuracy') 
   plt3.xlabel('epoch') 
   plt3.legend(['train', 'test'], loc='upper left') 
 
   plt3.subplot(212) 
   plt3.plot(history.history['loss']) 
   plt3.plot(history.history['val_loss']) 
   plt3.title('model loss') 
   plt3.ylabel('loss') 
   plt3.xlabel('epoch') 
   plt3.legend(['train', 'test'], loc='upper left') 
   plt3.show()
   plt3.savefig("plot-acculoss")
plot(history)
 
 
N = epochs
plt1.style.use("ggplot")
plt1.figure()
plt1.plot(np.arange(0, N), history.history["accuracy"], label="train_acc")
plt1.plot(np.arange(0, N), history.history["val_accuracy"], label="val_acc")
plt1.title("Training Accuracy on Dataset")
plt1.xlabel("Epoch #")
plt1.ylabel("Accuracy")
plt1.legend(loc="lower left")
plt1.savefig("plot_accuracy")
 
 
plt2.style.use("ggplot")
plt2.figure()
plt2.plot(np.arange(0, N), history.history["loss"], label="train_loss")
plt2.plot(np.arange(0, N), history.history["val_loss"], label="val_loss")
plt2.title("Training Loss on Dataset")
plt2.xlabel("Epoch #")
plt2.ylabel("Loss")
plt2.legend(loc="lower left")
plt2.savefig("plot_loss")
