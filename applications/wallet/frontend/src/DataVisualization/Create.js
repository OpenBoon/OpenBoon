import { colors, spacing, typography, constants } from '../Styles'

import FacetSvg from '../Icons/facet.svg'
import RangeSvg from '../Icons/range.svg'

import Button, { VARIANTS } from '../Button'

const ICON_SIZE = 22

const CHART_TYPES = [
  {
    icon: <FacetSvg width={ICON_SIZE} color={colors.structure.white} />,
    name: 'Facet',
    legend:
      'Shows the range of values and the number of each for one for a selected field.',
  },
  {
    icon: <RangeSvg width={ICON_SIZE} color={colors.structure.white} />,
    name: 'Range',
    legend:
      'Shows the min, max, mean, median, and mode for the numerical values of a selected field.',
  },
]

const DataVisualizationCreate = () => {
  return (
    <div css={{ flex: 1, display: 'flex', padding: spacing.base }}>
      <div
        css={{
          flex: 1,
          padding: spacing.normal,
          backgroundColor: colors.structure.lead,
        }}
      >
        <h2
          css={{
            fontSize: typography.size.large,
            lineHeight: typography.height.large,
          }}
        >
          Create a Data Visualization
        </h2>

        <p
          css={{
            margin: 0,
            paddingTop: spacing.base,
            paddingBottom: spacing.base,
          }}
        >
          Data visualizations are representations of specific information from
          dataset. Adjusting the filters will allow you to dynamically view the
          affect they have on the search results. Data visualization can be
          shared with other users in the projects by exporting and then
          uploading.
        </p>

        {CHART_TYPES.map(({ icon, name, legend }) => {
          return (
            <div
              key={name}
              css={{
                display: 'flex',
                paddingTop: spacing.normal,
                paddingBottom: spacing.normal,
                borderBottom: constants.borders.divider,
              }}
            >
              <div
                css={{
                  display: 'flex',
                  padding: spacing.moderate,
                  backgroundColor: colors.structure.iron,
                  borderRadius: constants.borderRadius.small,
                }}
              >
                {icon}
              </div>

              <div
                css={{
                  flex: 1,
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'center',
                  paddingLeft: spacing.normal,
                  paddingRight: spacing.normal,
                }}
              >
                <div
                  css={{
                    fontSize: typography.size.regular,
                    lineHeight: typography.height.regular,
                    fontWeight: typography.weight.medium,
                  }}
                >
                  {name}
                </div>
                <div
                  css={{
                    fontSize: typography.size.regular,
                    lineHeight: typography.height.regular,
                    fontWeight: typography.weight.medium,
                    color: colors.structure.zinc,
                  }}
                >
                  {legend}
                </div>
              </div>

              <div
                css={{
                  display: 'flex',
                  alignItems: 'center',
                }}
              >
                <Button
                  variant={VARIANTS.SECONDARY_SMALL}
                  onClick={console.warn}
                >
                  Create
                </Button>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default DataVisualizationCreate
