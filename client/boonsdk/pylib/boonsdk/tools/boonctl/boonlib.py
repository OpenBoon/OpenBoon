
import boonsdk

app = boonsdk.app_from_env()


def add_subparser(subparsers):
    subparser = subparsers.add_parser("boonlibs", help='Import BoonLibs')
    commands = subparser.add_subparsers()

    import_cmd = commands.add_parser('import', help='Import BoonLib')
    import_cmd.add_argument('name', metavar='NAME', help='The BoonLib name or ID')
    import_cmd.set_defaults(func=import_boonlib)

    list_cmd = commands.add_parser('list', help='List BoonLibs')
    list_cmd.set_defaults(func=display_list)


def display_list(args):
    fmt = '%-36s %-24s %-24s %-36s'
    print((fmt % ('ID', 'Name', 'Type', 'Desc')))
    print("-" * (36+24+24+36))
    for item in app.boonlibs.find_boonlibs():
        print(fmt % (item.id,
                     item.name,
                     item.entity.name + "/" + item.entity_type,
                     item.description))


def import_boonlib(args):
    print(app.boonlibs.import_boonlib(args.name))
