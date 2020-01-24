import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing, typography, colors } from '../Styles'

const BASE_STYLE = {
  fontSize: typography.size.large,
  lineHeight: typography.height.large,
  fontWeight: typography.weight.regular,
  paddingTop: spacing.comfy,
  paddingBottom: spacing.normal,
  paddingRight: spacing.small,
  color: colors.structure.steel,
}

const Breadcrumbs = ({ crumbs }) => {
  const {
    query: { projectId },
  } = useRouter()

  return (
    <div css={{ display: 'flex' }}>
      {crumbs.map(({ title, href }, index) => {
        const isLastCrumb = index === crumbs.length - 1

        if (!isLastCrumb) {
          return (
            <div key={title} css={{ display: 'flex' }}>
              <Link href={href} as={href.replace('[projectId]', projectId)}>
                <a
                  css={{
                    ...BASE_STYLE,
                    ':hover': {
                      textDecoration: 'none',
                      color: colors.structure.white,
                    },
                  }}>
                  {title}
                </a>
              </Link>
              <span css={{ ...BASE_STYLE }}>/</span>
            </div>
          )
        }
        return (
          <div
            key={title}
            css={{
              ...BASE_STYLE,
              color: colors.structure.white,
            }}>
            {title}
          </div>
        )
      })}
    </div>
  )
}

Breadcrumbs.propTypes = {
  crumbs: PropTypes.arrayOf(
    PropTypes.shape({
      title: PropTypes.string.isRequired,
      href: PropTypes.oneOfType([PropTypes.string, PropTypes.bool]).isRequired,
    }),
  ).isRequired,
}

export default Breadcrumbs
