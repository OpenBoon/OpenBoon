import PropTypes from 'prop-types'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

const SidebarLink = ({ href, as, children }) => {
  const { pathname } = useRouter()

  const isCurrentPage = pathname === href

  return (
    <li css={{ borderBottom: constants.borders.transparent }}>
      <Link href={href} as={as} passHref>
        <a
          css={{
            display: 'flex',
            alignItems: 'center',
            padding: spacing.moderate,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            backgroundColor: isCurrentPage ? colors.structure.smoke : 'none',
            color: isCurrentPage ? colors.key.one : colors.structure.steel,
            svg: {
              marginRight: spacing.moderate,
            },
            ':hover': {
              textDecoration: 'none',
              color: isCurrentPage ? colors.key.one : colors.structure.pebble,
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
  as: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default SidebarLink
