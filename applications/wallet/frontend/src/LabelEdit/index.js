import { useReducer } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { spacing } from '../Styles'

import Form from '../Form'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

const INITIAL_STATE = {
  success: false,
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const LabelEdit = ({ projectId, modelId, label }) => {
  const [state, dispatch] = useReducer(reducer, {
    ...INITIAL_STATE,
    label,
    newLabel: label,
  })

  return (
    <Form>
      {!!state.errors.global && (
        <div
          css={{
            display: 'flex',
            paddingTop: spacing.base,
            paddingBottom: spacing.base,
          }}
        >
          <FlashMessage variant={FLASH_VARIANTS.ERROR}>
            {state.errors.global}
          </FlashMessage>
        </div>
      )}

      <Input
        autoFocus
        id="newLabel"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Label"
        type="text"
        value={state.newLabel}
        onChange={({ target: { value } }) => dispatch({ newLabel: value })}
        hasError={state.errors.newLabel !== undefined}
        errorMessage={state.errors.newLabel}
      />

      <ButtonGroup>
        <Link
          href="/[projectId]/models/[modelId]"
          as={`/${projectId}/models/${modelId}`}
          passHref
        >
          <Button variant={BUTTON_VARIANTS.SECONDARY}>Cancel</Button>
        </Link>

        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ projectId, modelId, dispatch, state })}
          isDisabled={
            !state.label || state.newLabel === label || state.isLoading
          }
        >
          {state.isLoading ? 'Saving...' : 'Save Label Changes'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

LabelEdit.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
}

export default LabelEdit
