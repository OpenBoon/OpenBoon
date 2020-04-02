import { useReducer } from 'react'
import PropTypes from 'prop-types'
import Head from 'next/head'

import { colors, constants, typography, spacing } from '../Styles'

import LogoSvg from '../Icons/logo.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'

import { CURRENT_POLICIES_DATE, onSubmit } from './helpers'

const WIDTH = 446
const LOGO_WIDTH = 143

const INITIAL_STATE = {
  isChecked: false,
  errors: { global: '' },
}

const reducer = (state, action) => ({ ...state, ...action })

const Policies = ({ userId, setUser }) => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const { isChecked } = state

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
      }}
    >
      <Head>
        <title>Policies</title>
      </Head>

      <form
        method="post"
        onSubmit={(event) => event.preventDefault()}
        css={{
          display: 'flex',
          flexDirection: 'column',
          padding: spacing.colossal,
          width: WIDTH,
          backgroundColor: colors.structure.mattGrey,
          borderRadius: constants.borderRadius.small,
          boxShadow: constants.boxShadows.default,
        }}
      >
        <LogoSvg width={LOGO_WIDTH} css={{ alignSelf: 'center' }} />

        <h3
          css={{
            textAlign: 'center',
            fontSize: typography.size.large,
            lineHeight: typography.height.large,
            paddingTop: spacing.spacious,
          }}
        >
          Terms &amp; Conditions and Privacy Policy
        </h3>

        {state.errors.global && (
          <div css={{ paddingTop: spacing.spacious }}>
            <FlashMessage variant={FLASH_VARIANTS.ERROR}>
              {state.errors.global}
            </FlashMessage>
          </div>
        )}

        <p
          css={{
            margin: 0,
            paddingTop: spacing.spacious,
            color: colors.structure.zinc,
            textAlign: 'center',
            a: {
              color: colors.key.one,
            },
          }}
        >
          By accepting and clicking the &quot;Continue&quot; button you confirm
          that you have read and agree with Zorroa’s{' '}
          <a
            href={`/policies/${CURRENT_POLICIES_DATE}/terms-and-conditions.pdf`}
            target="_blank"
            rel="noopener noreferrer"
          >
            Terms&nbsp;and&nbsp;Conditions
          </a>{' '}
          and{' '}
          <a
            href={`/policies/${CURRENT_POLICIES_DATE}/privacy-policy.pdf`}
            target="_blank"
            rel="noopener noreferrer"
          >
            Privacy&nbsp;Policy
          </a>
          .
        </p>

        <div
          css={{
            paddingTop: spacing.normal,
            display: 'flex',
            justifyContent: 'center',
          }}
        >
          <Checkbox
            variant={CHECKBOX_VARIANTS.PRIMARY}
            option={{
              value: 'isChecked',
              label: 'Accept',
              initialValue: false,
              isDisabled: false,
            }}
            onClick={(value) => dispatch({ isChecked: value })}
          />
        </div>

        <div
          css={{
            paddingTop: spacing.normal,
            display: 'flex',
            justifyContent: 'center',
          }}
        >
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => onSubmit({ dispatch, userId, setUser })}
            isDisabled={!isChecked}
          >
            Continue
          </Button>
        </div>
      </form>
    </div>
  )
}

Policies.propTypes = {
  userId: PropTypes.number.isRequired,
  setUser: PropTypes.func.isRequired,
}

export { Policies as default, CURRENT_POLICIES_DATE }
