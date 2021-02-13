import PropTypes from 'prop-types'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

const SidebarLink = ({ projectId, href, children }) => {
  const { pathname } = useRouter()

  const isCurrentPage = pathname === href

  return (
    <li css={{ borderBottom: constants.borders.regular.transparent }}>
      <Link href={href} as={href.replace('[projectId]', projectId)} passHref>
        <a
          css={{
            display: 'flex',
            alignItems: 'center',
            padding: spacing.moderate,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            backgroundColor: isCurrentPage ? colors.structure.mattGrey : 'none',
            color: isCurrentPage ? colors.key.two : colors.structure.zinc,
            svg: { marginRight: spacing.moderate },
            ':hover': {
              textDecoration: 'none',
              color: colors.structure.white,
              backgroundColor: colors.structure.mattGrey,
            },
          }}
        >
          {children}
        </a>
      </Link>
    </li>
  )
}

SidebarLink.propTypes = {
  projectId: PropTypes.string.isRequired,
  href: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default SidebarLink
