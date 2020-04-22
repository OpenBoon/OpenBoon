import { useState } from 'react'
import PropTypes from 'prop-types'

import { typography, colors, spacing, constants } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import ChevronSvg from '../Icons/chevron.svg'

const CHEVRON_WIDTH = 20

const STYLES = {
  PRIMARY: {
    wrapper: { paddingTop: spacing.normal },
    backgroundColor: colors.structure.smoke,
    boxShadow: constants.boxShadows.default,
    verticalPadding: spacing.normal,
    childrenWrapper: {
      padding: spacing.spacious,
      paddingTop: spacing.normal,
    },
  },
  PANEL: {
    wrapper: {},
    backgroundColor: colors.structure.lead,
    titleWeight: typography.weight.regular,
    verticalPadding: spacing.moderate,
    childrenWrapper: {
      width: '100%',
      backgroundColor: colors.structure.coal,
    },
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Accordion = ({ variant, title, children, isInitiallyOpen }) => {
  const [isOpen, setOpen] = useState(isInitiallyOpen)

  return (
    <div css={STYLES[variant].wrapper}>
      <div
        css={{
          backgroundColor: STYLES[variant].backgroundColor,
          borderRadius: constants.borderRadius.small,
          boxShadow: STYLES[variant].boxShadow,
        }}
      >
        <div
          css={{
            borderBottom: constants.borders.tabs,
            paddingTop: STYLES[variant].verticalPadding,
            paddingBottom: STYLES[variant].verticalPadding,
            paddingLeft: spacing.moderate,
            display: 'flex',
          }}
        >
          <Button
            aria-label={`${isOpen ? 'Collapse' : 'Expand'} Section`}
            variant={BUTTON_VARIANTS.NEUTRAL}
            onClick={() => setOpen(!isOpen)}
          >
            <ChevronSvg
              width={CHEVRON_WIDTH}
              color={colors.structure.steel}
              css={{
                transform: isOpen ? 'rotate(-180deg)' : '',
                ':hover': {
                  color: colors.structure.white,
                },
              }}
            />
          </Button>
          <h4
            css={{
              fontSize: typography.size.medium,
              lineHeight: typography.height.medium,
              paddingLeft: spacing.moderate,
              display: 'flex',
              alignItems: 'center',
              fontWeight: STYLES[variant].titleWeight,
            }}
          >
            {title}
          </h4>
        </div>
        {isOpen && <div css={STYLES[variant].childrenWrapper}>{children}</div>}
      </div>
    </div>
  )
}

Accordion.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  title: PropTypes.node.isRequired,
  children: PropTypes.node.isRequired,
  isInitiallyOpen: PropTypes.bool.isRequired,
}

export default Accordion
