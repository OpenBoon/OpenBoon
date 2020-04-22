import PropTypes from 'prop-types'

import { spacing, typography, colors, constants } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Button, { VARIANTS } from '../Button'

const FiltersMenu = ({
  projectId,
  assetId,
  filters,
  fields,
  setIsMenuOpen,
}) => {
  return (
    <div css={{ overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
      <div css={{ overflowX: 'auto' }}>
        {Object.entries(fields).map(([key, value]) => (
          <Accordion
            key={key}
            variant={ACCORDION_VARIANTS.PANEL}
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
                          key={subSubKey}
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
      <div css={{ padding: spacing.base, display: 'flex' }}>
        <Button
          variant={VARIANTS.SECONDARY}
          onClick={() => setIsMenuOpen(false)}
          style={{ flex: 1 }}
        >
          x Cancel
        </Button>
        <div css={{ width: spacing.base }} />
        <Button
          variant={VARIANTS.PRIMARY}
          onClick={() => setIsMenuOpen(false)}
          style={{ flex: 1 }}
        >
          + Add Selected Filters
        </Button>
      </div>
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
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersMenu
