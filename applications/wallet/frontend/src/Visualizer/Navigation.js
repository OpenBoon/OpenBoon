import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, constants, spacing, typography } from '../Styles'

import { getQueryString } from '../Fetch/helpers'
import { cleanup } from '../Filters/helpers'

import AssetsSvg from '../Icons/assets.svg'
import ChartsSvg from '../Icons/charts.svg'

const FROM = 0
const SIZE = 100

const VisualizerNavigation = () => {
  const {
    pathname,
    query: { projectId, query },
  } = useRouter()

  const q = cleanup({ query })

  const { data: { count: itemCount = -1 } = {} } = useSWR(
    `/api/v1/projects/${projectId}/searches/query/?query=${q}&from=${FROM}&size=${SIZE}`,
    {
      suspense: false,
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )

  return (
    <div
      css={{
        backgroundColor: colors.structure.lead,
        color: colors.structure.steel,
        margin: spacing.hairline,
        marginTop: 0,
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
            paddingLeft: spacing.moderate,
            paddingRight: spacing.moderate,
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
          as={`/${projectId}/visualizer${getQueryString({ query })}`}
          passHref
        >
          <a
            css={{
              whiteSpace: 'nowrap',
              color:
                pathname === '/[projectId]/visualizer'
                  ? colors.key.two
                  : colors.structure.steel,
            }}
          >
            <AssetsSvg height={constants.icons.regular} /> Assets
          </a>
        </Link>

        <div
          css={{
            marginTop: spacing.base,
            marginBottom: spacing.base,
            borderLeft: constants.borders.medium.coal,
          }}
        />

        <Link
          href={{
            pathname: '/[projectId]/visualizer/data-visualization',
            query: { projectId, query },
          }}
          as={`/${projectId}/visualizer/data-visualization${getQueryString({
            query,
          })}`}
          passHref
        >
          <a
            css={{
              whiteSpace: 'nowrap',
              color:
                pathname === '/[projectId]/visualizer/data-visualization'
                  ? colors.key.two
                  : colors.structure.steel,
            }}
          >
            <ChartsSvg height={constants.icons.regular} /> Data Visualization
          </a>
        </Link>
      </div>

      {itemCount > -1 && (
        <div
          css={{
            whiteSpace: 'nowrap',
            padding: spacing.base,
            paddingLeft: spacing.moderate,
            paddingRight: spacing.moderate,
            fontFamily: typography.family.condensed,
          }}
        >
          {itemCount} Assets
        </div>
      )}
    </div>
  )
}

export default VisualizerNavigation
