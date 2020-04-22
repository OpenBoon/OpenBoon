import PropTypes from 'prop-types'

import { spacing, typography, colors, constants } from '../Styles'

import Accordion, { VARIANTS } from '../Accordion'

const FiltersMenu = ({ projectId, assetId, filters, fields }) => {
  return (
    <div css={{ overflowX: 'auto' }}>
      {Object.entries(fields).map(([key, value]) => (
        <Accordion
          key={key}
          variant={VARIANTS.PANEL}
          title={key}
          isInitiallyOpen
        >
          {Array.isArray(value) ? (
            <div
              css={{
                padding: spacing.moderate,
                color: colors.structure.zinc,
              }}
            >
              &amp; {key}
            </div>
          ) : (
            Object.entries(value).map(([subKey, subValue], index) => (
              <div
                key={subKey}
                css={{
                  padding: spacing.moderate,
                  borderTop: index !== 0 ? constants.borders.divider : '',
                }}
              >
                {Array.isArray(subValue) ? (
                  <div css={{ color: colors.structure.zinc }}>
                    &amp; {subKey}
                  </div>
                ) : (
                  <div>
                    <h4
                      css={{
                        fontFamily: 'Roboto Mono',
                        fontWeight: typography.weight.regular,
                        paddingTop: spacing.moderate,
                      }}
                    >
                      {subKey}
                    </h4>
                    {Object.entries(subValue).map(([subSubKey], i) => (
                      <div
                        css={{
                          padding: spacing.moderate,
                          color: colors.structure.zinc,
                          borderBottom:
                            i !== 0 ? constants.borders.divider : '',
                        }}
                      >
                        &amp; {subSubKey}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))
          )}
        </Accordion>
      ))}
    </div>
  )
}

FiltersMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(
    PropTypes.shape({
      type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.object])
        .isRequired,
    }).isRequired,
  ).isRequired,
  fields: PropTypes.objectOf(PropTypes.objectOf).isRequired,
}

export default FiltersMenu
