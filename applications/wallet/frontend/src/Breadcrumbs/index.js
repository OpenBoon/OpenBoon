import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing, typography, colors } from '../Styles'

import BetaBadge from '../BetaBadge'
import PageTitle from '../PageTitle'

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
  const { query } = useRouter()

  return (
    <div css={{ display: 'flex', flexShrink: 0 }}>
      {crumbs.map(({ title, href, isBeta = false }, index) => {
        const isLastCrumb = index === crumbs.length - 1

        if (!isLastCrumb) {
          return (
            <div key={title} css={{ display: 'flex' }}>
              {isBeta && (
                <div
                  css={{
                    display: 'flex',
                    alignItems: 'center',
                    paddingTop: spacing.comfy,
                    paddingBottom: spacing.normal,
                  }}
                >
                  <BetaBadge isLeft />
                </div>
              )}

              <Link
                href={href}
                as={href
                  .split('/')
                  .map((s) =>
                    s.replace(/\[(.*)\]/gi, (_, group) => query[group]),
                  )
                  .join('/')}
                passHref
              >
                <a
                  css={{
                    ...BASE_STYLE,
                    ':hover': {
                      textDecoration: 'none',
                      color: colors.structure.white,
                    },
                  }}
                >
                  {title}
                </a>
              </Link>
              <span css={{ ...BASE_STYLE }}>/</span>
            </div>
          )
        }
        return (
          <PageTitle key={title}>
            {isBeta && <BetaBadge isLeft />}
            {title}
          </PageTitle>
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
      isBeta: PropTypes.bool,
    }),
  ).isRequired,
}

export default Breadcrumbs
