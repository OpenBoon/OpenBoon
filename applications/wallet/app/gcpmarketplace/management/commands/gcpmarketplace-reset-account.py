from django.core.management import BaseCommand

from gcpmarketplace.utils import get_procurement_api


class Command(BaseCommand):
    help = """Resets a GCP marketplace account. Sometimes the wallet server can get out of sync
    with the Marketplace account server. If a GCP Account is marked as active but is not
    tracked correctly in Wallet it can lead to an infinite loop in the GCP Marektplace
    integration waiting for an account to be activated. This tool resets the account on the
    GCP side and will trigger the setup process on the Wallet side.

    Args:
        account_name(str): Complete account name path to reset.

    """

    def add_arguments(self, parser):
        parser.add_argument('account_name', type=str)

    def handle(self, *args, **options):
        print(f'Resetting {options["account_name"]}')
        request = get_procurement_api().providers().accounts().reset(name=options['account_name'])
        request.execute()
        print('Success.')
