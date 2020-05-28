import namegenerator
from random_word import RandomWords


def random_project_name():
    """Random name generator to use as the default for projects if a name is not given."""
    from projects.models import Project
    name = namegenerator.gen()
    if Project.objects.filter(name=name).exists():
        return random_project_name()
    return name
