import PropTypes from 'prop-types'
import Link from 'next/link'

import { spacing, typography, colors } from '../Styles'

const Breadcrumbs = ({ crumbs }) => {
  return (
    <div css={{ display: 'flex' }}>
      {crumbs.map(({ title, href }, index) => {
        const isLastCrumb = index === crumbs.length - 1
        const content = !isLastCrumb ? `${title} /` : `${title}`
        return (
          <Link key={title} href={href}>
            <div
              css={{
                fontSize: typography.size.large,
                lineHeight: typography.height.large,
                fontWeight: typography.weight.regular,
                paddingTop: spacing.comfy,
                paddingBottom: spacing.normal,
                paddingRight: isLastCrumb ? 0 : spacing.small,
                color: isLastCrumb
                  ? colors.structure.white
                  : colors.structure.steel,
                ':hover': {
                  color: colors.structure.white,
                },
              }}>
              {content}
            </div>
          </Link>
        )
      })}
    </div>
  )
}

Breadcrumbs.propTypes = {
  crumbs: PropTypes.arrayOf(
    PropTypes.shape({
      title: PropTypes.string.isRequired,
      href: PropTypes.string.isRequired,
    }),
  ).isRequired,
}

export default Breadcrumbs
