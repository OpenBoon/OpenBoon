import { useState } from 'react'
import PropTypes from 'prop-types'

import { typography, colors, spacing, constants } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import ChevronSvg from '../Icons/chevron.svg'

const CHEVRON_WIDTH = 20

const STYLES = {
  PRIMARY: {
    container: {
      backgroundColor: colors.structure.smoke,
      borderRadius: constants.borderRadius.small,
    },
    title: {
      borderBottom: constants.borders.tabs,
      paddingTop: spacing.normal,
      paddingBottom: spacing.normal,
      paddingLeft: spacing.moderate,
      display: 'flex',
      h4: {
        fontWeight: typography.weight.bold,
      },
    },
    content: {
      padding: spacing.spacious,
      paddingTop: spacing.normal,
    },
  },
  PANEL: {
    container: {
      backgroundColor: colors.structure.lead,
      borderRadius: constants.borderRadius.small,
      ':last-of-type > div': {
        borderBottom: constants.borders.tabs,
      },
    },
    title: {
      borderTop: constants.borders.tabs,
      paddingTop: spacing.moderate,
      paddingBottom: spacing.moderate,
      paddingLeft: spacing.moderate,
      display: 'flex',
      h4: {
        fontSize: typography.size.regular,
        fontWeight: typography.weight.regular,
      },
    },
    content: {
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
    <div css={STYLES[variant].container}>
      <div css={STYLES[variant].title}>
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
              ':hover': { color: colors.structure.white },
            }}
          />
        </Button>
        <h4
          css={{
            fontSize: typography.size.medium,
            lineHeight: typography.height.medium,
            paddingLeft: spacing.moderate,
            display: 'flex',
          }}
        >
          {title}
        </h4>
      </div>
      {isOpen && <div css={STYLES[variant].content}>{children}</div>}
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
