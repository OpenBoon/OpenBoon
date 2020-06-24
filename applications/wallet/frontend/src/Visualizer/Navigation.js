import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { colors, constants, spacing, typography } from '../Styles'

import { formatUrl } from '../Filters/helpers'

import AssetsSvg from '../Icons/assets.svg'
import ChartsSvg from '../Icons/charts.svg'

const ICON_SIZE = 20

const VisualizerNavigation = ({ itemCount }) => {
  const {
    pathname,
    query: { projectId, query },
  } = useRouter()

  return (
    <div
      css={{
        paddingLeft: spacing.base,
        paddingRight: spacing.base,
        backgroundColor: colors.structure.lead,
        color: colors.structure.steel,
        boxShadow: constants.boxShadows.navBar,
        marginBottom: spacing.hairline,
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}
    >
      <div
        css={{
          display: 'flex',
          a: {
            textTransform: 'uppercase',
            fontWeight: typography.weight.medium,
            padding: spacing.base,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            ':hover': {
              textDecoration: 'none',
              color: colors.structure.white,
              backgroundColor: colors.structure.mattGrey,
            },
          },
          svg: { marginRight: spacing.base },
        }}
      >
        <Link
          href={{
            pathname: '/[projectId]/visualizer',
            query: { projectId, query },
          }}
          as={`/${projectId}/visualizer${formatUrl({ query })}`}
          passHref
        >
          <a
            css={{
              color:
                pathname === '/[projectId]/visualizer'
                  ? colors.key.one
                  : colors.structure.steel,
            }}
          >
            <AssetsSvg width={ICON_SIZE} aria-hidden /> Assets
          </a>
        </Link>

        <div
          css={{
            marginTop: spacing.base,
            marginBottom: spacing.base,
            borderLeft: constants.borders.spacer,
          }}
        />

        <Link
          href={{
            pathname: '/[projectId]/visualizer/data-visualization',
            query: { projectId, query },
          }}
          as={`/${projectId}/visualizer/data-visualization${formatUrl({
            query,
          })}`}
          passHref
        >
          <a
            css={{
              color:
                pathname === '/[projectId]/visualizer/data-visualization'
                  ? colors.key.one
                  : colors.structure.steel,
            }}
          >
            <ChartsSvg width={ICON_SIZE} aria-hidden /> Data Visualization
          </a>
        </Link>
      </div>

      <div
        css={{
          padding: spacing.base,
          fontFamily: typography.family.condensed,
        }}
      >
        {itemCount} Assets
      </div>
    </div>
  )
}

VisualizerNavigation.propTypes = {
  itemCount: PropTypes.number.isRequired,
}

export default VisualizerNavigation
