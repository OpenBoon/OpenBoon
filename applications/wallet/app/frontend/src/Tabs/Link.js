import PropTypes from 'prop-types'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, spacing, typography } from '../Styles'

const TabsLink = ({ title, href }) => {
  const { pathname } = useRouter()

  return (
    <li css={{ paddingRight: spacing.normal }}>
      <Link href={href} passHref>
        <a
          css={{
            border: `0 ${colors.green2} solid`,
            borderBottomWidth: href === pathname ? 2 : 0,
            color: href === pathname ? colors.white : colors.grey2,
            display: 'flex',
            alignItems: 'center',
            paddingTop: spacing.base,
            paddingBottom: spacing.base,
            textTransform: 'uppercase',
            fontSize: typography.size.kilo,
            lineHeight: typography.height.kilo,
            fontWeight: typography.weight.bold,
            ':hover': {
              textDecoration: 'none',
              color: href === pathname ? colors.white : colors.key.one,
            },
          }}>
          {title}
        </a>
      </Link>
    </li>
  )
}

TabsLink.propTypes = {
  title: PropTypes.string.isRequired,
  href: PropTypes.string.isRequired,
}

export default TabsLink
