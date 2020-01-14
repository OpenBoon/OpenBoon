import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'

import AccountProfileForm from './Form'

const AccountProfile = () => {
  return (
    <div>
      <Head>
        <title>Account Profile</title>
      </Head>

      <PageTitle>Account Profile</PageTitle>

      <Tabs
        tabs={[
          { title: 'Profile', href: '/[projectId]/account/profile' },
          { title: 'Change Password', href: '/[projectId]/account/password' },
        ]}
      />

      <AccountProfileForm onSubmit={console.warn} />
    </div>
  )
}

export default AccountProfile
