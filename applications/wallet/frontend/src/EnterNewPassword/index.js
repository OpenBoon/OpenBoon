import { useState } from 'react'
import Head from 'next/head'
import { useRouter } from 'next/router'

import { getUser } from '../Authentication/helpers'

import { spacing } from '../Styles'

import Form from '../Form'
import FormAlert from '../FormAlert'
import PageTitle from '../PageTitle'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Navbar from '../Navbar'
import SectionTitle from '../SectionTitle'

import { onSubmit } from './helpers'

export const noop = () => () => {}

const BUTTON_WIDTH = 95

const EnterNewPassword = () => {
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [confirmError, setConfirmError] = useState('')
  const [submitError, setSubmitError] = useState('')

  const {
    query: { uid, token },
  } = useRouter()

  const { email } = getUser()

  const mismatchingEmails = confirmPassword
    ? newPassword !== confirmPassword
    : false

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-start',
        paddingLeft: spacing.giant,
        paddingTop: spacing.colossal,
      }}>
      <Head>
        <title>Enter New Password</title>
      </Head>

      <Navbar isSidebarOpen={false} setSidebarOpen={noop}>
        <div />
      </Navbar>

      <PageTitle>{`User ID: ${email}`}</PageTitle>

      <Form>
        {submitError && (
          <FormAlert
            errorMessage={submitError}
            setErrorMessage={() => setSubmitError('')}
          />
        )}

        <SectionTitle>Enter New Password</SectionTitle>

        <Input
          id="newPassword"
          variant={INPUT_VARIANTS.SECONDARY}
          label="Password"
          type="password"
          value={newPassword}
          onChange={({ target: { value } }) => setNewPassword(value)}
          hasError={false}
          errorMessage=""
        />
        <Input
          id="confirmPassword"
          variant={INPUT_VARIANTS.SECONDARY}
          label="Confirm Password"
          type="password"
          value={confirmPassword}
          onChange={({ target: { value } }) => setConfirmPassword(value)}
          hasError={mismatchingEmails}
          errorMessage={confirmError}
          onBlur={() => {
            if (newPassword !== confirmPassword) {
              setConfirmError('Passwords do not match')
            }
            if (newPassword === confirmPassword) {
              setConfirmError('')
            }
          }}
        />

        <div css={{ width: BUTTON_WIDTH }}>
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() =>
              onSubmit({
                newPassword,
                confirmPassword,
                uid,
                token,
                setSubmitError,
              })
            }
            isDisabled={
              !newPassword ||
              !confirmPassword ||
              newPassword !== confirmPassword
            }>
            Save
          </Button>
        </div>
      </Form>
    </div>
  )
}

export default EnterNewPassword
