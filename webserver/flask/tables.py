from flask_table import Table, Col, LinkCol

class ResultsAdmin(Table):
    id = Col('Id')
    Disease = Col('Disease')
    Confidence = Col('Confidence')
    Breed = Col('Breed')
    Sex = Col('Sex')
    AgeMonth = Col('AgeMonth')
    Filename = Col('Filename')
    Time= Col('Time')
    #edit = LinkCol('Edit', 'edit', url_kwargs=dict(id='id'))


class Results(Table):
    id = Col('Id', show=False)
    Disease = Col('Disease')
    Confidence = Col('Confidence')
    Breed = Col('Breed')
    Sex = Col('Sex')
    AgeMonth = Col('AgeMonth')
    Filename = Col('Filename', show=False)
    Time= Col('Time', show=False)
