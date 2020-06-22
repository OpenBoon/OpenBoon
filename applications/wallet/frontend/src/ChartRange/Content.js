import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants, spacing, typography, colors } from '../Styles'

import chartShape from '../Chart/shape'

import { encode, ACTIONS, dispatch } from '../Filters/helpers'
import Button, { VARIANTS } from '../Button'

import FilterSvg from '../Icons/filter.svg'

const ICON_SIZE = 20

const ChartRangeContent = ({ chart: { type, attribute } }) => {
  const {
    pathname,
    query: { projectId, query },
  } = useRouter()

  const { data } = useSWR(
    // TODO: Update endpoint
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encode({
      filters: { type, attribute },
    })}`,
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )

  const { results } = data || {}

  return (
    <div
      css={{
        display: 'flex',
        flexWrap: 'wrap',
        '> div': {
          display: 'flex',
          flexDirection: 'column',
          width: `calc(${(1 / 3) * 100}% - ${(spacing.normal * 2) / 3}px)`,
          marginRight: spacing.normal,
          paddingBottom: spacing.normal,
          ':nth-of-type(3n)': { marginRight: 0 },
        },
      }}
    >
      {Object.entries(results).map(([key, value]) => (
        <div key={key}>
          <div
            css={{
              textTransform: 'uppercase',
              fontFamily: typography.family.condensed,
              color: colors.structure.zinc,
              paddingBottom: spacing.small,
            }}
          >
            {key}
          </div>
          <div
            css={{
              border: constants.borders.divider,
              borderRadius: constants.borderRadius.small,
              padding: spacing.moderate,
              paddingLeft: spacing.normal,
              paddingRight: spacing.normal,
            }}
          >
            {value.toLocaleString()}
          </div>
        </div>
      ))}
      <div>
        <div
          css={{
            height: typography.height.regular,
            marginBottom: spacing.small,
          }}
        >
          &nbsp;
        </div>
        <div
          css={{
            flex: 1,
            display: 'flex',
            alignItems: 'stretch',
            justifyContent: 'stretch',
          }}
        >
          <Button
            aria-label="Add Chart"
            variant={VARIANTS.NEUTRAL}
            onClick={() => {
              dispatch({
                type: ACTIONS.ADD_FILTER,
                payload: {
                  pathname,
                  projectId,
                  filter: { type, attribute, values: {} },
                  query,
                },
              })
            }}
            css={{
              flex: 1,
              display: 'flex',
              // padding: spacing.normal,
              color: colors.structure.zinc,
              ':hover, :focus': {
                color: colors.structure.white,
              },
            }}
          >
            <div css={{ display: 'flex', alignItems: 'center' }}>
              <div css={{ display: 'flex', paddingRight: spacing.small }}>
                <FilterSvg width={ICON_SIZE} />
              </div>
              Add Filter
            </div>
          </Button>
        </div>
      </div>
    </div>
  )
}

ChartRangeContent.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartRangeContent
