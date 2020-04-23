import PropTypes from 'prop-types'

import { spacing, typography, colors, constants } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import Button, { VARIANTS } from '../Button'

const ICON_SIZE = 16
const BOX_PADDING = spacing.moderate
const LABEL_LEFT_PADDING = spacing.normal
const OFFSET = ICON_SIZE + BOX_PADDING + LABEL_LEFT_PADDING

const FiltersMenu = ({
  // projectId,
  // assetId,
  // filters,
  fields,
  setIsMenuOpen,
}) => {
  return (
    <div
      css={{
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        flex: 1,
      }}
    >
      <div css={{ overflowX: 'auto' }}>
        {Object.entries(fields).map(([key, value]) =>
          Array.isArray(value) ? null : (
            <Accordion
              key={key}
              variant={ACCORDION_VARIANTS.PANEL}
              title={key}
              isInitiallyOpen={false}
            >
              {Object.entries(value).map(([subKey, subValue], index) => (
                <div
                  key={subKey}
                  css={{
                    padding: spacing.moderate,
                    borderTop:
                      index !== 0 && !Array.isArray(subValue)
                        ? constants.borders.divider
                        : '',
                  }}
                >
                  {Array.isArray(subValue) ? (
                    <div css={{ color: colors.structure.zinc }}>
                      <Checkbox
                        key={subKey}
                        variant={CHECKBOX_VARIANTS.SMALL}
                        option={{
                          value: subKey,
                          label: subKey,
                          initialValue: false,
                          isDisabled: false,
                        }}
                        onClick={console.warn}
                      />
                    </div>
                  ) : (
                    <div>
                      <h4
                        css={{
                          fontFamily: 'Roboto Mono',
                          fontWeight: typography.weight.regular,
                        }}
                      >
                        {subKey}
                      </h4>
                      {Object.entries(subValue).map(([subSubKey], i) => (
                        <div
                          key={subSubKey}
                          css={{
                            marginLeft: OFFSET,
                            borderTop: i !== 0 ? constants.borders.divider : '',
                          }}
                        >
                          <div
                            css={{
                              padding: BOX_PADDING,
                              color: colors.structure.zinc,
                              marginLeft: -OFFSET,
                            }}
                          >
                            <Checkbox
                              key={subSubKey}
                              variant={CHECKBOX_VARIANTS.SMALL}
                              option={{
                                value: subSubKey,
                                label: subSubKey,
                                initialValue: false,
                                isDisabled: false,
                              }}
                              onClick={console.warn}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </Accordion>
          ),
        )}
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
  // projectId: PropTypes.string.isRequired,
  // assetId: PropTypes.string.isRequired,
  // filters: PropTypes.arrayOf(
  //   PropTypes.shape({
  //     type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
  //     value: PropTypes.oneOfType([PropTypes.string, PropTypes.object])
  //       .isRequired,
  //   }).isRequired,
  // ).isRequired,
  fields: PropTypes.objectOf(PropTypes.objectOf).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersMenu
