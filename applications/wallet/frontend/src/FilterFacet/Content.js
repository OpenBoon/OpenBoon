import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

const FilterFacet = ({
  projectId,
  //   assetId,
  //   filters,
  //   filter,
  filter: { type, attribute },
  //   filterIndex,
}) => {
  const encodedFilter = btoa(JSON.stringify({ type, attribute }))

  const {
    data: {
      results: { buckets },
    },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
  )

  const { docCount: largestCount } = buckets[0]

  return (
    <>
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          paddingTop: spacing.base,
          paddingBottom: spacing.base,
          fontFamily: 'Roboto Condensed',
          color: colors.structure.zinc,
        }}
      >
        <div>KEYWORD</div>
        <div>COUNT</div>
      </div>
      <ul css={{ margin: 0, padding: 0, listStyle: 'none' }}>
        {buckets.map(({ key, docCount }) => {
          const offset = Math.max(1, (docCount * 100) / largestCount)

          return (
            <li key={key}>
              <Button
                style={{
                  width: '100%',
                  flexDirection: 'row',
                  ':hover': {
                    backgroundColor: colors.signal.electricBlue.background,
                  },
                }}
                variant={VARIANTS.NEUTRAL}
                onClick={() => {}}
              >
                <div css={{ width: '100%' }}>
                  <div css={{ display: 'flex' }}>
                    <div
                      css={{
                        width: `${offset}%`,
                        borderTop: constants.borders.facet,
                      }}
                    />
                    <div
                      css={{
                        height: 4,
                        width: `${100 - offset}%`,
                        borderTop: constants.borders.divider,
                      }}
                    />
                  </div>
                  <div
                    css={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      padding: spacing.base,
                    }}
                  >
                    <div>{key}</div>
                    <div>{docCount}</div>
                  </div>
                </div>
              </Button>
            </li>
          )
        })}
      </ul>
    </>
  )
}

FilterFacet.propTypes = {
  projectId: PropTypes.string.isRequired,
  //   assetId: PropTypes.string.isRequired,
  //   filters: PropTypes.arrayOf(
  //     PropTypes.shape({
  //       type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
  //       attribute: PropTypes.string,
  //       values: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  //     }).isRequired,
  //   ).isRequired,
  filter: PropTypes.shape({
    type: PropTypes.oneOf(['facet']).isRequired,
    attribute: PropTypes.string.isRequired,
    values: PropTypes.shape({ exists: PropTypes.bool }),
  }).isRequired,
  //   filterIndex: PropTypes.number.isRequired,
}

export default FilterFacet
