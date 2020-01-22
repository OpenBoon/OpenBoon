import PropTypes from 'prop-types'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

const SidebarLink = ({ href, children }) => {
  const {
    pathname,
    query: { projectId },
  } = useRouter()

  const isCurrentPage = pathname === `/[projectId]${href}`

  return (
    <li css={{ borderBottom: constants.borders.transparent }}>
      <Link href={`/[projectId]${href}`} as={`/${projectId}${href}`} passHref>
        <a
          css={{
            display: 'flex',
            alignItems: 'center',
            padding: spacing.moderate,
            fontSize: typography.size.hecto,
            lineHeight: typography.height.hecto,
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
  children: PropTypes.node.isRequired,
}

export default SidebarLink
