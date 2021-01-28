import namegenerator


def random_project_name():
    """Random name generator to use as the default for projects if a name is not given."""
    from projects.models import Project
    name = namegenerator.gen()
    if Project.all_objects.filter(name=name).exists():
        return random_project_name()
    return name
