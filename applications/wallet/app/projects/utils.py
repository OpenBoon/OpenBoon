import namegenerator

from organizations.models import Organization


def random_project_name():
    """Random name generator to use as the default for projects if a name is not given."""
    from projects.models import Project
    name = namegenerator.gen()
    if Project.all_objects.filter(name=name).exists():
        return random_project_name()
    return name


def is_user_project_organization_owner(user, project):
    """
    Returns true if the user is an owner of a project's organization.

    Args:
        user(User): User to test.
        project(Project): Project to test.

    Returns:
        bool: True if the user an organization owner for the project.

    """
    return Organization.objects.filter(projects=project, owners=user).exists()
