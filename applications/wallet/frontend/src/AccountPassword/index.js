import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'

import AccountPasswordForm from './Form'

const AccountPassword = () => {
  return (
    <>
      <Head>
        <title>Account Profile</title>
      </Head>

      <PageTitle>Account Profile</PageTitle>

      <Tabs
        tabs={[
          { title: 'Profile', href: '/[projectId]/account' },
          { title: 'Change Password', href: '/[projectId]/account/password' },
        ]}
      />

      <AccountPasswordForm />
    </>
  )
}

export default AccountPassword
