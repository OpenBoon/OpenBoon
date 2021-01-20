import namegenerator


def random_organization_name():
    """Random name generator to use as the default for projects if a name is not given."""
    from projects.models import Project
    name = namegenerator.gen()
    if Project.all_objects.filter(name=name).exists():
        return random_organization_name()
    return name
