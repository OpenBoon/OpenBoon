import { useReducer } from 'react'
import PropTypes from 'prop-types'
import Head from 'next/head'

import { colors, constants, typography, spacing } from '../Styles'

import Logo from '../Icons/logo.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import FlashMessageErrors from '../FlashMessage/Errors'

import { CURRENT_POLICIES_DATE, onSubmit } from './helpers'

import PoliciesForm from './Form'

const WIDTH = 446
const LOGO_WIDTH = 180

const INITIAL_STATE = {
  isChecked: false,
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const Policies = ({ userId }) => {
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
        <Logo width={LOGO_WIDTH} css={{ alignSelf: 'center' }} />

        <h3
          css={{
            textAlign: 'center',
            fontSize: typography.size.large,
            lineHeight: typography.height.large,
            paddingTop: spacing.spacious,
          }}
        >
          Terms of Use and Privacy Policy
        </h3>

        <FlashMessageErrors
          errors={state.errors}
          styles={{ paddingTop: spacing.spacious }}
        />

        <div
          css={{
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
          }}
        >
          <PoliciesForm dispatch={dispatch} />
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
            onClick={() => onSubmit({ dispatch, userId })}
            isDisabled={!isChecked || state.isLoading}
          >
            {state.isLoading ? 'Loading...' : 'Continue'}
          </Button>
        </div>
      </form>
    </div>
  )
}

Policies.propTypes = {
  userId: PropTypes.number.isRequired,
}

export { Policies as default, CURRENT_POLICIES_DATE }
