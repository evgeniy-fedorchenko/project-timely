package com.efedorchenko.timely.model

object Model {

    private const val LOCAL = "[a-zA-Z0-9][a-zA-Z0-9._-]{0,62}[a-zA-Z0-9]"
    private const val SUBDOMAIN = "([a-zA-Z0-9][a-zA-Z0-9_-]{1,14}\\.)"
    private const val TLD = "([a-z]{2,4})"

    private const val EMAIL_REGEX = "^(?!.*[-._]{2})$LOCAL@$SUBDOMAIN{1,2}$TLD$"
    private const val PASSWORD_REGEX = "^(?!.*(.)\\1{3,})(?=.*[A-ZА-Я])(?=.*[a-zа-я])(?=.*\\d)[A-Za-zА-Яа-я0-9!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?\\\\]{8,64}$"

    /**
     * Регулярное выражение для проверки пароля
     *
     * Требования к паролю:
     * - Длина: от 8 до 64 символов
     * - Обязательно должен содержать:
     *     - Минимум одну заглавную букву (латиницу или кириллицу)
     *     - Минимум одну строчную букву (латиницу или кириллицу)
     *     - Минимум одну цифру
     * - Может содержать спецсимволы: `!@#$%^&*()_+-=[]{}|;:,.<>?\`
     * - Запрещены:
     *     - Любые пробельные символы
     *     - Более трех одинаковых символов подряд
     *
     * Примеры:
     * - Правильно: `Password123!`, `Пароль123#`, `StrongP@ssw0rd`
     * - Неправильно:
     *     - `pass` (слишком короткий)
     *     - `password` (нет заглавной буквы и цифры)
     *     - `Password 123` (содержит пробел)
     *     - `Passsssword123` (больше трех одинаковых символов подряд)
     */
    fun isPasswordValid(password: String): Boolean {
        return password.matches(PASSWORD_REGEX.toRegex())
    }

    /**
     * Регулярное выражение для проверки email-адреса
     *
     * Требования к email:
     * - Локальная часть (до `@`):
     *     - Может содержать буквы (латнициа, любой регистр), цифры и символы `._-`
     *     - Не может начинаться или кончаться на символы `._-`
     *     - Длина: от 2 до 64 символа (границы включительно)
     *     - Не может содержать несколько символов из `._-` подряд
     *
     * - Доменная часть (после `@`):
     *     - Может содержать 1-2 поддомена
     *     - Не может содержать несколько символов из `_-` подряд
     *     - Поддомены и домен верхнего уровня должны разделяться точкой
     *     - Поддомен может содержать латиницу в любом регистре, цифры и символы `_-`
     *     - Поддомен не может начинаться или заканчиваться на символ `_-`
     *     - Каждый поддомен должен иметь длину от 2 до 15 символов
     *     - Домен верхнего уровня должен состоять только из латиницы (нижный регистр)
     *     - Длина домена верхнего уровня - от 2 до 4 символов (границы включительно)
     *
     * Примеры:
     * - Правильно:
     *     - `user@domain.com`
     *     - `user.name@my-c0mpany.d0ma-in.com`
     *     - `user@sub.domain.com`
     *     - `123user@domain.co.uk`
     *
     * - Неправильно:
     *     - `@domain.com` (нет локальной части)
     *     - `user@.com` (домен начинается с точки)
     *     - `user@domain` (нет домена верхнего уровня)
     *     - `user@sub.sub.domain.com` (слишком много поддоменов)
     *     - `user@sub-.domain.com` (слишком много поддомен кончается на тире)
     *     - `user@domain.toolong` (слишком длинный домен верхнего уровня)
     */
    fun isLoginValid(login: String): Boolean {
        return login.matches(EMAIL_REGEX.toRegex())
    }

    fun isLoginPairValid(loginPair: Pair<String, String>): Boolean {
        return isLoginValid(loginPair.first) && isPasswordValid(loginPair.second)
    }

    fun isRepeatPasswordValid(repeatPassword: String, password: String): Boolean {
        return password == repeatPassword && isPasswordValid(repeatPassword)
    }

    fun isNameValid(name: String): Boolean {
        return name.isNotEmpty() && name.length < 255
    }

    fun isSpaceKeyValid(spaceKey: String): Boolean {
        return spaceKey.isNotEmpty() && spaceKey.length < 36
    }

    fun isPositionValid(position: String): Boolean {
        return position.isNotEmpty() && position.length < 128
    }
}