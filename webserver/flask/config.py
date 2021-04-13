import os
basedir = os.path.abspath(os.path.dirname(__file__))

S3_BUCKET = os.environ.get("S3_BUCKET")
S3_KEY = os.environ.get("S3_KEY")
S3_SECRET = os.environ.get("S3_SECRET_ACCESS_KEY")



class Config(object):
    DEBUG = False
    TESTING = False
    CSRF_ENABLED = True
    SECRET_KEY = 'this-really-needs-to-be-changed'
    #SQLALCHEMY_DATABASE_URI = os.environ['DATABASE_URL']
    UPLOAD_FOLDER = '/tmp/'
    S3_KEY = 'fu2sYwfCqgl52vQianGSvdWEkFgAQq6UKLuCQlpq'
    #S3_BUCKET = 'elasticbeanstalk-ap-southeast-1-694305119167'
    S3_BUCKET = 'caninetensorflask-bucket'
    #S3_KEY = os.environ.get('S3_KEY')
    S3_SECRET_ACCESS_KEY = 'AKIAINW5QQ64CAXLEY7Q'
    S3_LOCATION = 'http://{}.s3.amazonaws.com/'.format(S3_BUCKET)


class ProductionConfig(Config):
    DEBUG = False


class StagingConfig(Config):
    DEVELOPMENT = True
    DEBUG = True


class DevelopmentConfig(Config):
    DEVELOPMENT = True
    DEBUG = True


class TestingConfig(Config):
    TESTING = True




    
