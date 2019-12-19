import PropTypes from 'prop-types'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

const SidebarLink = ({ href, children }) => {
  const { pathname } = useRouter()

  return (
    <li css={{ borderBottom: constants.borders.transparent }}>
      <Link href={href} passHref>
        <a
          css={{
            display: 'flex',
            alignItems: 'center',
            padding: spacing.moderate,
            fontSize: typography.size.kilo,
            lineHeight: typography.height.kilo,
            backgroundColor:
              href === pathname ? colors.structure.smoke : 'none',
            color: href === pathname ? colors.key.one : colors.structure.steel,
            svg: {
              marginRight: spacing.moderate,
            },
            ':hover': {
              textDecoration: 'none',
              color:
                href === pathname ? colors.key.one : colors.structure.pebble,
              backgroundColor: colors.structure.smoke,
            },
          }}>
          {children}
        </a>
      </Link>
    </li>
  )
}

SidebarLink.propTypes = {
  href: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default SidebarLink
