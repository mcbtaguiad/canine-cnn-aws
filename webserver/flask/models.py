from app import db

class Canine(db.Model):
    __tablename__ = 'canine'

    id = db.Column(db.Integer, primary_key=True)
    
    Disease= db.Column(db.String())
    Confidence  = db.Column(db.String())
    Breed = db.Column(db.String())
    Sex = db.Column(db.String())
    AgeMonth = db.Column(db.String())
    Filename = db.Column(db.String())
    Time = db.Column(db.String())

    def __init__(self, Disease, Confidence, Breed, Sex, AgeMonth, Filename, Time):
        #self.label = label
        self.Disease = Disease
        self.Confidence = Confidence
        self.Breed= Breed
        self.Sex= Sex
        self.AgeMonth = AgeMonth
        self.Filename = Filename
        self.Time = Time

    def __repr__(self):
        return '<id {}>'.format(self.id)
    
    def serialize(self):
        return {
            'id': self.id, 
            #'label': self.label,
            'Disease': self.Disease,
            'Confidence': self.Confidence,
            'Breed': self.Breed,
            'Sex':self.Sex,
            'AgeMonth':self.AgeMonth,
            'Filename':self.Filename,
            'Time':self.Time
        }
