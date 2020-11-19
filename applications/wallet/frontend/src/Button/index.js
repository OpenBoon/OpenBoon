/* eslint-disable react/jsx-props-no-spreading */
import { forwardRef } from 'react'
import PropTypes from 'prop-types'

import stylesShape from '../Styles/shape'

import { typography, spacing, colors, constants } from '../Styles'

const BASE = ({ isDisabled }) => ({
  flex: 0,
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  borderRadius: constants.borderRadius.small,
  padding: `${spacing.moderate}px ${spacing.spacious}px`,
  whiteSpace: 'nowrap',
  fontSize: typography.size.regular,
  lineHeight: typography.height.regular,
  fontWeight: typography.weight.medium,
  border: 'none',
  cursor: isDisabled ? 'not-allowed' : 'pointer',
  color: colors.structure.white,
  backgroundColor: colors.structure.transparent,
})

const STYLES = {
  PRIMARY: {
    '&,&:hover,&:visited': {
      backgroundColor: colors.key.one,
    },
    '&:hover': {
      textDecoration: 'none',
      backgroundColor: colors.key.two,
    },
    '&[aria-disabled=true]': {
      color: colors.structure.mattGrey,
      backgroundColor: colors.structure.steel,
    },
  },
  PRIMARY_SMALL: {
    padding: `${spacing.base}px ${spacing.normal}px`,
    '&,&:hover,&:visited': {
      backgroundColor: colors.key.one,
    },
    '&:hover': {
      textDecoration: 'none',
      backgroundColor: colors.key.two,
    },
    '&[aria-disabled=true]': {
      color: colors.structure.mattGrey,
      backgroundColor: colors.structure.steel,
    },
  },
  SECONDARY: {
    '&,&:hover,&:visited': {
      backgroundColor: colors.structure.steel,
    },
    '&:hover': {
      textDecoration: 'none',
      backgroundColor: colors.structure.zinc,
    },
    '&[aria-disabled=true]': {
      color: colors.structure.mattGrey,
      backgroundColor: colors.structure.steel,
    },
  },
  SECONDARY_SMALL: {
    padding: `${spacing.base}px ${spacing.normal}px`,
    '&,&:hover,&:visited': {
      backgroundColor: colors.structure.steel,
    },
    '&:hover': {
      textDecoration: 'none',
      backgroundColor: colors.structure.zinc,
    },
    '&[aria-disabled=true]': {
      color: colors.structure.mattGrey,
      backgroundColor: colors.structure.steel,
    },
  },
  WARNING: {
    '&,&:hover,&:visited': {
      backgroundColor: colors.signal.warning.base,
    },
    '&:hover': {
      textDecoration: 'none',
      opacity: constants.opacity.half,
    },
    '&[aria-disabled=true]': {
      color: colors.structure.mattGrey,
      backgroundColor: colors.structure.steel,
    },
  },
  LINK: {
    padding: spacing.small,
    fontWeight: typography.weight.regular,
    '&,&:hover,&:visited': {
      color: colors.key.one,
    },
    '&:hover': {
      textDecoration: 'underline',
    },
  },
  NEUTRAL: {
    padding: 0,
  },
  MENU: {
    flex: 1,
    padding: spacing.base,
    paddingLeft: spacing.normal,
    color: colors.key.one,
    fontWeight: typography.weight.regular,
    height: '100%',
    ':hover, &.focus-visible:focus': {
      textDecoration: 'none',
      backgroundColor: colors.structure.smoke,
    },
  },
  MENU_ITEM: {
    flex: '1',
    alignItems: 'flex-start',
    padding: `${spacing.base}px ${spacing.normal}px`,
    fontWeight: typography.weight.regular,
    borderRadius: 0,
    ':hover, &.focus-visible:focus': {
      textDecoration: 'none',
      backgroundColor: colors.structure.iron,
    },
  },
  ICON: {
    padding: spacing.base,
    color: colors.structure.steel,
    ':hover, &.focus-visible:focus': {
      textDecoration: 'none',
      color: colors.structure.white,
      svg: {
        opacity: 1,
      },
    },
    '&[aria-disabled=true]': {
      color: colors.structure.steel,
    },
  },
  MICRO: {
    padding: spacing.small,
    paddingLeft: spacing.moderate / 2,
    paddingRight: spacing.moderate / 2,
    borderRadius: spacing.mini,
    backgroundColor: colors.structure.smoke,
    color: colors.structure.zinc,
    lineHeight: typography.height.regular,
    fontSize: typography.size.regular,
    fontFamily: typography.family.condensed,
    textTransform: 'uppercase',
    ':hover, &.focus-visible:focus': {
      textDecoration: 'none',
      color: colors.structure.white,
    },
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Button = forwardRef(
  (
    { variant, children, href, style, isDisabled, onClick, target, ...props },
    ref,
  ) => {
    const Element = href && !isDisabled ? 'a' : 'button'

    const disabled = isDisabled ? { 'aria-disabled': true } : {}

    const addedProps =
      href && !isDisabled
        ? {
            href,
            onClick,
            target,
            rel:
              target && target === '_blank' ? 'noopener noreferrer' : undefined,
          }
        : {
            type: 'button',
            ...disabled,
            onClick: (event) => {
              if (isDisabled) return event.preventDefault()
              return onClick(event)
            },
          }

    return (
      <Element
        ref={ref}
        css={{ ...BASE({ isDisabled }), ...STYLES[variant], ...style }}
        {...addedProps}
        {...props}
      >
        {children}
      </Element>
    )
  },
)

Button.defaultProps = {
  href: false,
  style: {},
  isDisabled: false,
  onClick: undefined,
  target: undefined,
}

Button.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  children: PropTypes.node.isRequired,
  href: PropTypes.oneOfType([PropTypes.string, PropTypes.bool]),
  style: stylesShape,
  isDisabled: PropTypes.bool,
  onClick: PropTypes.func,
  target: PropTypes.string,
}

export default Button
